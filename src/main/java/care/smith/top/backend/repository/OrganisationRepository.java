package care.smith.top.backend.repository;

import care.smith.top.backend.model.OrganisationDao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganisationRepository
    extends PagingAndSortingRepository<OrganisationDao, String> {
  Slice<OrganisationDao> findAllByNameIsContainingIgnoreCaseOrDescriptionIsContainingIgnoreCase(
      String name, String description, Pageable pageable);

  default Slice<OrganisationDao> findAllByNameOrDescription(String name, Pageable pageable) {
    if (name == null) return findAll(pageable);
    return findAllByNameIsContainingIgnoreCaseOrDescriptionIsContainingIgnoreCase(
        name, name, pageable);
  }
}
