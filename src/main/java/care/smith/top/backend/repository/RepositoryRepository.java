package care.smith.top.backend.repository;

import care.smith.top.backend.model.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends PagingAndSortingRepository<Repository, String> {
  Slice<Repository> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Slice<Repository> findByNameContainingAndOrganisationId(
      String name, String organisationId, Pageable pageable);
}
