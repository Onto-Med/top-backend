package care.smith.top.backend.repository;

import care.smith.top.backend.model.QueryDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryRepository extends JpaRepository<QueryDao, UUID> {
  Page<QueryDao> findAllByRepository_OrganisationIdAndRepositoryIdOrderByIdDesc(
      String organisationId, String repositoryId, Pageable pagea);

  Optional<QueryDao> findByRepository_OrganisationIdAndRepositoryIdAndId(
      String organisationId, String repositoryId, UUID id);
}
