package care.smith.top.backend.service;

import care.smith.top.backend.model.Role;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService implements ContentService, UserDetailsService {
  @Autowired private UserRepository userRepository;

  @Override
  public long count() {
    return userRepository.count();
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException(username));
  }

  /**
   * This method takes the JWT token of the current request and checks of a user with equal ID
   * (OAuth2 subject ID) exists. If a user exists, it's username will be updated by JWT claim
   * "preferred_username", "username" or subject ID. If no user was found, a new user will be
   * created.
   *
   * <p>If authentication is disabled by property, this method will return `null`.
   *
   * @return UserDetails of the currently authenticated user.
   */
  @PreAuthorize("isAuthenticated()")
  public UserDao getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return null;

    Jwt jwt = (Jwt) auth.getPrincipal();
    Optional<UserDao> existingUser = userRepository.findById(jwt.getSubject());

    String username =
        StringUtils.defaultIfBlank(
            jwt.getClaimAsString("preferred_username"),
            StringUtils.defaultIfBlank(jwt.getClaimAsString("username"), jwt.getSubject()));

    if (existingUser.isPresent()) {
      UserDao user = existingUser.get();
      boolean modified = false;

      if (!user.getUsername().equals(username)) {
        user.username(username);
        modified = true;
      }
      if (user.getRole() == null) {
        user.role(Role.defaultValue());
        modified = true;
      }
      if (modified) {
        return userRepository.save(user);
      }
      return user;
    }

    return userRepository.save(new UserDao(jwt.getSubject(), username));
  }
}
