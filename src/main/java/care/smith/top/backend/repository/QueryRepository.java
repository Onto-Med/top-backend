package care.smith.top.backend.repository;

import care.smith.top.backend.model.QueryDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<QueryDao, String> {
  Page<QueryDao> findAllByRepository_OrganisationIdAndRepositoryId(
      String organisationId, String repositoryId, Pageable pagea);
}
