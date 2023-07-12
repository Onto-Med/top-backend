package care.smith.top.backend.repository;

import care.smith.top.backend.model.OrganisationMembershipDao;
import care.smith.top.backend.model.Permission;
import care.smith.top.backend.model.key.OrganisationMembershipKeyDao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationMembershipRepository
    extends JpaRepository<OrganisationMembershipDao, OrganisationMembershipKeyDao> {
  boolean
      existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
          String organisationId, String userId, List<Permission> permissions);

  List<OrganisationMembershipDao> findAllByOrganisation_Id(String organisationId);
}
