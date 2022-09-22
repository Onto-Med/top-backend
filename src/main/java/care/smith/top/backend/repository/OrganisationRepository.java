package care.smith.top.backend.repository;

import care.smith.top.backend.model.Organisation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface OrganisationRepository extends PagingAndSortingRepository<Organisation, String> {
  Slice<Organisation> findAllByNameIsContainingIgnoreCaseOrDescriptionIsContainingIgnoreCase(
      String name, String description, Pageable pageable);
}
