package care.smith.top.backend.repository;

import care.smith.top.backend.model.Organisation;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface OrganisationRepository extends PagingAndSortingRepository<Organisation, String> {
  Collection<Organisation> findAllByNameOrDescription(String name, String description);
}
