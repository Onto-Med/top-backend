package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryRepository extends PagingAndSortingRepository<Directory, String> {
  // TODO: not working, maybe query must be specified:
  Page<Directory> findAllByName(String name, Pageable pageable);
}
