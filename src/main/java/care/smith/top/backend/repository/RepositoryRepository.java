package care.smith.top.backend.repository;

import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.model.RepositoryType;
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

  default Slice<RepositoryDao> findByNameAndPrimaryAndRepositoryType(
      String name, Boolean primary, RepositoryType repositoryType, Pageable pageable) {
    if (name == null) {
      if (primary == null && repositoryType == null) return findAll(pageable);
      if (primary == null) return findAllByRepositoryType(repositoryType, pageable);
      if (repositoryType == null) return findAllByPrimary(primary, pageable);
      return findAllByPrimaryAndRepositoryType(primary, repositoryType, pageable);
    }
    if (primary == null && repositoryType == null)
      return findByNameContainingIgnoreCase(name, pageable);
    if (primary == null)
      return findByNameContainingIgnoreCaseAndRepositoryType(name, repositoryType, pageable);
    if (repositoryType == null)
      return findByNameContainingIgnoreCaseAndPrimary(name, primary, pageable);
    return findByNameContainingIgnoreCaseAndPrimaryAndRepositoryType(
        name, primary, repositoryType, pageable);
  }

  Slice<RepositoryDao> findAllByPrimaryAndRepositoryType(
      Boolean primary, RepositoryType repositoryType, Pageable pageable);

  Slice<RepositoryDao> findAllByRepositoryType(RepositoryType repositoryType, Pageable pageable);

  Slice<RepositoryDao> findByNameContainingIgnoreCaseAndRepositoryType(
      String name, RepositoryType repositoryType, Pageable pageable);

  Slice<RepositoryDao> findByNameContainingIgnoreCaseAndPrimaryAndRepositoryType(
      String name, Boolean primary, RepositoryType repositoryType, Pageable pageable);

  default Slice<RepositoryDao> findByOrganisationIdAndNameAndRepositoryType(
      String organisationId, String name, RepositoryType repositoryType, Pageable pageable) {
    if (name == null && repositoryType == null)
      return findByOrganisationId(organisationId, pageable);
    if (name == null)
      return findByOrganisationIdAndRepositoryType(organisationId, repositoryType, pageable);
    if (repositoryType == null)
      return findByOrganisationIdAndNameContainingIgnoreCase(organisationId, name, pageable);
    return findByOrganisationIdAndNameContainingIgnoreCaseAndRepositoryType(
        organisationId, name, repositoryType, pageable);
  }

  Slice<RepositoryDao> findByOrganisationIdAndNameContainingIgnoreCaseAndRepositoryType(
      String organisationId, String name, RepositoryType repositoryType, Pageable pageable);

  Slice<RepositoryDao> findByOrganisationIdAndRepositoryType(
      String organisationId, RepositoryType repositoryType, Pageable pageable);

  Optional<RepositoryDao> findByIdAndOrganisationId(String repositoryId, String organisationId);
}
