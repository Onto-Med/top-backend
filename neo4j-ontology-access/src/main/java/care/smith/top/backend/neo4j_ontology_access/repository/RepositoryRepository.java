package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.ClassRelation;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends PagingAndSortingRepository<Repository, String> {

  @Query(
      "MATCH (r:Repository { id: $repositoryId }) -[:BELONGS_TO]-> (d:Directory { id: $superDirectoryId }) "
          + "RETURN r")
  Optional<Repository> findByIdAndSuperDirectoryId(
      @Param("repositoryId") String repositoryId,
      @Param("superDirectoryId") String superDirectoryId);

  @Query(
      "MATCH (cr:ClassRelation) "
          + "WHERE id(cr) = $classRelation.__id__ "
          + "MATCH (r:Repository { id: $repository.__id__ }) "
          + "CREATE (cr) -[:BELONGS_TO]-> (r) "
          + "RETURN r")
  Repository addClassRelation(
      @Param("repository") Repository repository,
      @Param("classRelation") ClassRelation classRelation);
}
