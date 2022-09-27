package care.smith.top.backend.repository;

import care.smith.top.backend.model.RepositoryDao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryRepository extends PagingAndSortingRepository<RepositoryDao, String> {
  Slice<RepositoryDao> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Slice<RepositoryDao> findAllByPrimary(Boolean primary, Pageable pageable);

  Slice<RepositoryDao> findByNameContainingIgnoreCaseAndPrimary(
      String name, Boolean primary, Pageable pageable);

  Slice<RepositoryDao> findByOrganisationId(String organisationId, Pageable pageable);

  Slice<RepositoryDao> findByOrganisationIdAndNameContainingIgnoreCase(
      String organisationId, String name, Pageable pageable);
}
