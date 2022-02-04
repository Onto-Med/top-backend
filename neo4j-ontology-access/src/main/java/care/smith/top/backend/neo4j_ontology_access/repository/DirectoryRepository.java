package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryRepository extends PagingAndSortingRepository<Directory, String> {
  Streamable<Directory> findByNameContainingIgnoreCase(String name);

  Streamable<Directory> findByDescriptionContainingIgnoreCase(String description);
}
