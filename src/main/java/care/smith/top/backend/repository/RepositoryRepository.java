package care.smith.top.backend.repository;

import care.smith.top.backend.model.RepositoryDao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryDao, String> {
  Slice<RepositoryDao> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Slice<RepositoryDao> findAllByPrimary(Boolean primary, Pageable pageable);

  Slice<RepositoryDao> findByNameContainingIgnoreCaseAndPrimary(
      String name, Boolean primary, Pageable pageable);

  Slice<RepositoryDao> findByOrganisationId(String organisationId, Pageable pageable);

  Slice<RepositoryDao> findByOrganisationIdAndNameContainingIgnoreCase(
      String organisationId, String name, Pageable pageable);

  default Slice<RepositoryDao> findByNameAndPrimary(
      String name, Boolean primary, Pageable pageable) {
    if (name == null) {
      if (primary == null) return findAll(pageable);
      return findAllByPrimary(primary, pageable);
    }
    if (primary == null) return findByNameContainingIgnoreCase(name, pageable);
    return findByNameContainingIgnoreCaseAndPrimary(name, primary, pageable);
  }

  default Slice<RepositoryDao> findByOrganisationIdAndName(
      String organisationId, String name, Pageable pageable) {
    if (name == null) return findByOrganisationId(organisationId, pageable);
    return findByOrganisationIdAndNameContainingIgnoreCase(organisationId, name, pageable);
  }

  Optional<RepositoryDao> findByIdAndOrganisationId(String repositoryId, String organisationId);
}
