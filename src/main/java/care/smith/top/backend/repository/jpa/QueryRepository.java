package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.QueryDao;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<QueryDao, String> {
  Page<QueryDao> findAllByRepository_OrganisationIdAndRepositoryIdOrderByCreatedAtDesc(
      String organisationId, String repositoryId, Pageable pageable);

  Optional<QueryDao> findByRepository_OrganisationIdAndRepositoryIdAndId(
      String organisationId, String repositoryId, String id);
}
