package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.OrganisationMembershipDao;
import care.smith.top.backend.model.jpa.Permission;
import care.smith.top.backend.model.jpa.key.OrganisationMembershipKeyDao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganisationMembershipRepository
    extends JpaRepository<OrganisationMembershipDao, OrganisationMembershipKeyDao> {
  boolean
      existsById_OrganisationIdAndId_UserIdAndPermissionInAndUser_EnabledTrueAndUser_LockedFalse(
          String organisationId, String userId, List<Permission> permissions);

  List<OrganisationMembershipDao> findAllByOrganisation_Id(String organisationId);
}
