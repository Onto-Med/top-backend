package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryRepository extends PagingAndSortingRepository<Directory, String> {
  @Query(
      "MATCH (d:Directory) "
          + "WHERE d.name =~ '(?i).*' + $name + '.*' "
          + "OPTIONAL MATCH p = (d) -[:BELONGS_TO]-> (:Directory) "
          + "RETURN d, relationships(p), nodes(p)")
  Streamable<Directory> findByNameContainingIgnoreCase(@Param("name") String name);

  @Query(
      "MATCH (d:Directory) "
          + "WHERE d.description =~ '(?i).*' + $description + '.*' "
          + "OPTIONAL MATCH p = (d) -[:BELONGS_TO]-> (:Directory) "
          + "RETURN d, relationships(p), nodes(p)")
  Streamable<Directory> findByDescriptionContainingIgnoreCase(
      @Param("description") String description);

  @Query(
      "MATCH (d:Directory) "
          + "WHERE $type IN labels(d) "
          + "OPTIONAL MATCH p = (d) -[:BELONGS_TO]-> (:Directory) "
          + "RETURN d, relationships(p), nodes(p)")
  Streamable<Directory> findByType(@Param("type") String type);
}
