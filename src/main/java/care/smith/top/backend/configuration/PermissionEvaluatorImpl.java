package care.smith.top.backend.configuration;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.Permission;
import care.smith.top.backend.model.Role;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.repository.OrganisationMembershipRepository;
import care.smith.top.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

@Configuration
public class PermissionEvaluatorImpl implements PermissionEvaluator {
  @Autowired private OrganisationMembershipRepository organisationMembershipDaoRepository;
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

    if (targetType.equals(OrganisationDao.class.getName())) {
      return organisationMembershipDaoRepository
          .existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
              targetId.toString(),
              user.getId(),
              Permission.getIncluding(Permission.valueOf((String) permission)));
    }
    return false;
  }
}
