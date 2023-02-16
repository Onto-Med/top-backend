package care.smith.top.backend.service;

import care.smith.top.backend.model.Role;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
@Transactional
public class UserService implements ContentService, UserDetailsService {
  @Autowired private UserRepository userRepository;

  @Value("${spring.security.oauth2.enabled}")
  private Boolean oauth2Enabled;

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
   * This method checks if a user with equal ID (OAuth2 JWT token subject ID) exists. If that is the
   * case, it's username will be updated by JWT claim "preferred_username", "username" or subject
   * ID. If no user was found, a new user will be created.
   *
   * <p>If authentication is disabled by property, this method will return {@link Optional#empty()}.
   *
   * @return UserDetails of the currently authenticated user.
   */
  @Transactional
  public Optional<UserDao> getOrCreateUser(@Nonnull Jwt jwt) {
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
        return Optional.of(userRepository.save(user));
      }
      return Optional.of(user);
    }

    return Optional.of(
        userRepository.save(
            new UserDao(jwt.getSubject(), username)
                .role(userRepository.count() == 0 ? Role.ADMIN : Role.defaultValue())));
  }

  @Nullable
  public UserDao getCurrentUser() {
    if (!oauth2Enabled) return null;
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof UserDao) return (UserDao) principal;
    return new UserDao("anonymous_user", "Anonymous User").role(Role.ANONYMOUS);
  }
}
