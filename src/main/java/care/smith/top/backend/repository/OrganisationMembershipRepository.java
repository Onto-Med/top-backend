package care.smith.top.backend.repository;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.OrganisationMembershipDao;
import care.smith.top.backend.model.Permission;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.model.key.OrganisationMembershipKeyDao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganisationMembershipRepository
    extends JpaRepository<OrganisationMembershipDao, OrganisationMembershipKeyDao> {
  boolean
      existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
          String organisationId, String userId, List<Permission> permissions);
}
