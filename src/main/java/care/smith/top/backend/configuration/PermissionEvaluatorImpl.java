package care.smith.top.backend.configuration;

import care.smith.top.backend.model.*;
import care.smith.top.backend.repository.OrganisationMembershipRepository;
import care.smith.top.backend.repository.RepositoryRepository;
import care.smith.top.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Optional;

@Configuration
public class PermissionEvaluatorImpl implements PermissionEvaluator {
  @Autowired private OrganisationMembershipRepository organisationMembershipDaoRepository;
  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private UserService userService;

  @Value("${spring.security.oauth2.enabled}")
  private Boolean oauth2Enabled;

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    if (targetDomainObject instanceof OrganisationDao) {
      return hasPermission(
          authentication,
          ((OrganisationDao) targetDomainObject).getId(),
          OrganisationDao.class.getName(),
          permission);
    }
    return false;
  }

  /**
   * This voter will vote for accept, if the user has the administrator ("ADMIN") role or at least
   * the specified permission for the target object.
   *
   * @param authentication Token of an authentication request.
   * @param targetId ID of the target object, permissions shall be checked for.
   * @param targetType Full class name of the target object.
   * @param permission Required permission.
   * @return `true`, if voting for accept.
   */
  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    UserDao user = userService.getCurrentUser();
    if (user == null) return !oauth2Enabled;
    if (user.getRole().equals(Role.ADMIN)) return true;

    String id = targetId.toString();
    Permission perm = Permission.valueOf((String) permission);

    if (targetType.equals(OrganisationDao.class.getName())) {
      return voteForOrganisation(user.getId(), id, perm);
    }
    if (targetType.equals(RepositoryDao.class.getName())) {
      return voteForRepository(user.getId(), id, perm);
    }

    return false;
  }

  private boolean voteForOrganisation(String userId, String organisationId, Permission permission) {
    return organisationMembershipDaoRepository
        .existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
            organisationId, userId, Permission.getIncluding(permission));
  }

  private boolean voteForRepository(String userId, String repositoryId, Permission permission) {
    Optional<RepositoryDao> repository = repositoryRepository.findById(repositoryId);
    if (repository.isEmpty()) return true;
    if (repository.get().getPrimary() && permission.equals(Permission.READ)) return true;
    return voteForOrganisation(userId, repository.get().getOrganisation().getId(), permission);
  }
}
