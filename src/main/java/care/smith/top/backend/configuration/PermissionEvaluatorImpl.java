package care.smith.top.backend.configuration;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.Permission;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.repository.OrganisationMembershipRepository;
import care.smith.top.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

@Configuration
public class PermissionEvaluatorImpl implements PermissionEvaluator {
  @Autowired private OrganisationMembershipRepository organisationMembershipDaoRepository;
  @Autowired private UserService userService;

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    return false;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    if (targetType.equals(OrganisationDao.class.getName())) {
      UserDao user = userService.getCurrentUser();
      return organisationMembershipDaoRepository
          .existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
              targetId.toString(),
              user.getId(),
              Permission.getIncluding(Permission.valueOf((String) permission)));
    }
    return false;
  }
}
