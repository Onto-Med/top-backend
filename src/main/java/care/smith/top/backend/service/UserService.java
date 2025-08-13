package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.model.jpa.key.OrganisationMembershipKeyDao;
import care.smith.top.backend.repository.jpa.OrganisationMembershipRepository;
import care.smith.top.backend.repository.jpa.OrganisationRepository;
import care.smith.top.backend.repository.jpa.RepositoryRepository;
import care.smith.top.backend.repository.jpa.UserRepository;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService implements ContentService, UserDetailsService {
  @Autowired private OrganisationMembershipRepository organisationMembershipRepository;
  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private UserRepository userRepository;

  @Value("${spring.security.oauth2.enabled}")
  private Boolean oauth2Enabled;

  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private OrganisationRepository organisationRepository;

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
  public Optional<UserDao> getOrCreateUser(@NotNull Jwt jwt) {
    Optional<UserDao> existingUser = userRepository.findById(jwt.getSubject());

    String username =
        StringUtils.defaultIfBlank(
            jwt.getClaimAsString("name"),
            StringUtils.defaultIfBlank(
                jwt.getClaimAsString("preferred_username"), jwt.getSubject()));

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

  public Optional<UserDao> getUserById(String userId) {
    return userRepository.findById(userId);
  }

  public Page<UserDao> getUsers(String name, List<String> organisationIds, Integer page) {
    PageRequest pageRequest =
        PageRequest.of(
            page != null ? page - 1 : 0, pageSize, Sort.by(UserDao_.USERNAME, UserDao_.ID));
    return userRepository.findAllByUsernameAndOrganisationIds(name, organisationIds, pageRequest);
  }

  @Nullable
  public UserDao getCurrentUser() {
    if (!oauth2Enabled) return null;
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof UserDao) return (UserDao) principal;
    return new UserDao("anonymous_user", "Anonymous User").role(Role.ANONYMOUS);
  }

  @Nullable
  @Transactional
  public OrganisationMembershipDao grantMembership(
      @NotNull OrganisationDao organisation,
      @NotNull UserDao user,
      @NotNull Permission permission) {
    UserDao currentUser = getCurrentUser();
    if (!hasPermission(
        currentUser, organisation.getId(), OrganisationDao.class.getName(), Permission.MANAGE))
      throw new InsufficientAuthenticationException(
          String.format(
              "User '%s' has insufficient permissions for organisation '%s'!",
              currentUser == null ? "anonymous_user" : currentUser.getId(), organisation.getId()));

    OrganisationMembershipDao membership = organisation.setMemberPermission(user, permission);
    organisationRepository.save(organisation);
    return membership;
  }

  @Transactional
  public void revokeMembership(@NotNull OrganisationDao organisation, @NotNull UserDao user) {
    UserDao currentUser = getCurrentUser();
    if (!hasPermission(
        currentUser, organisation.getId(), OrganisationDao.class.getName(), Permission.MANAGE))
      throw new InsufficientAuthenticationException(
          String.format(
              "User '%s' has insufficient permissions for organisation '%s'!",
              currentUser == null ? "anonymous_user" : currentUser.getId(), organisation.getId()));

    organisationMembershipRepository
        .findById(new OrganisationMembershipKeyDao(user.getId(), organisation.getId()))
        .ifPresent(m -> organisationMembershipRepository.delete(m));
  }

  public boolean hasPermission(
      UserDao user, Serializable targetId, String targetType, Permission permission) {
    if (user == null) return !oauth2Enabled;
    if (!user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked())
      return false;

    if (user.getRole().equals(Role.ADMIN)) return true;

    if (targetType.equals(OrganisationDao.class.getName())) {
      return hasOrganisationPermission(user, targetId.toString(), permission);
    }
    if (targetType.equals(RepositoryDao.class.getName())) {
      return hasRepositoryPermission(user, targetId.toString(), permission);
    }

    return false;
  }

  private boolean hasOrganisationPermission(
      UserDao user, String organisationId, Permission permission) {
    return organisationMembershipRepository
        .existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
            organisationId, user.getId(), Permission.getIncluding(permission));
  }

  private boolean hasRepositoryPermission(
      UserDao user, String repositoryId, Permission permission) {
    Optional<RepositoryDao> repository = repositoryRepository.findById(repositoryId);
    if (repository.isEmpty()) return true;
    if (repository.get().getPrimary() && permission.equals(Permission.READ)) return true;
    return hasOrganisationPermission(user, repository.get().getOrganisation().getId(), permission);
  }
}
