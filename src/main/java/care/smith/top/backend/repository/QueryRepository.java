package care.smith.top.backend.repository;

import care.smith.top.backend.model.QueryDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QueryRepository extends JpaRepository<QueryDao, String> {
  Page<QueryDao> findAllByRepository_OrganisationIdAndRepositoryIdOrderByCreatedAtDesc(
      String organisationId, String repositoryId, Pageable pageable);

  Optional<QueryDao> findByRepository_OrganisationIdAndRepositoryIdAndId(
      String organisationId, String repositoryId, String id);
}
