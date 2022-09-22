package care.smith.top.backend.repository;

import care.smith.top.backend.model.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.net.ContentHandler;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends PagingAndSortingRepository<Repository, String> {
  Slice<Repository> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Slice<Repository> findAllByPrimary(Boolean primary, Pageable pageable);

  Slice<Repository> findByNameContainingIgnoreCaseAndPrimary(
      String name, Boolean primary, Pageable pageable);

  Slice<Repository> findByOrganisationId(String organisationId, Pageable pageable);

  Slice<Repository> findByOrganisationIdAndNameContainingIgnoreCase(
      String organisationId, String name, Pageable pageable);
}
