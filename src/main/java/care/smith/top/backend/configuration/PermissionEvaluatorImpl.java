package care.smith.top.backend.configuration;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.Permission;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.service.UserService;
import java.io.Serializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

@Configuration
public class PermissionEvaluatorImpl implements PermissionEvaluator {
  @Autowired private UserService userService;

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
    if (targetDomainObject instanceof RepositoryDao) {
      return hasPermission(
          authentication,
          ((RepositoryDao) targetDomainObject).getId(),
          RepositoryDao.class.getName(),
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

    String id = targetId.toString();
    Permission perm = Permission.valueOf((String) permission);

    return userService.hasPermission(user, id, targetType, perm);
  }
}
