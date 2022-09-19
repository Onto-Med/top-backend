package care.smith.top.backend.repository;

import care.smith.top.backend.model.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends PagingAndSortingRepository<Repository, String> {
  Optional<Repository> findByIdAndOrganisationId(String repositoryId, String organisationId);

  Slice<Repository> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Slice<Repository> findByNameContainingAndOrganisationId(
      String name, String organisationId, Pageable pageable);
}
