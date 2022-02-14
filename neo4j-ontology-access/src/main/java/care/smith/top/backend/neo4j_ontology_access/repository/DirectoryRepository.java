package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryRepository extends PagingAndSortingRepository<Directory, String> {
  Streamable<Directory> findByNameContainingIgnoreCase(String name);

  Streamable<Directory> findByDescriptionContainingIgnoreCase(String description);

  @Query(
    "MATCH (d:Directory) "
    + "WHERE $type IN labels(d) "
    + "RETURN d"
  )
  Streamable<Directory> findByType(@Param("type") String type);
}
