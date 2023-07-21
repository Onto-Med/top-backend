package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.OrganisationDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganisationRepository extends JpaRepository<OrganisationDao, String> {
  Page<OrganisationDao> findAllByNameIsContainingIgnoreCaseOrDescriptionIsContainingIgnoreCase(
      String name, String description, Pageable pageable);

  default Page<OrganisationDao> findAllByNameOrDescription(String name, Pageable pageable) {
    if (name == null) return findAll(pageable);
    return findAllByNameIsContainingIgnoreCaseOrDescriptionIsContainingIgnoreCase(
        name, name, pageable);
  }
}
