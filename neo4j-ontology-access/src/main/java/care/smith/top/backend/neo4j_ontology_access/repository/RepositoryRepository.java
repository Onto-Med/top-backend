package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends PagingAndSortingRepository<Repository, String> {

  /**
   * Returns a {@link Repository} object by repositoryId and directoryId. The repository must have a
   * direct or transitive BELONGS_TO relationship to a {@link
   * care.smith.top.backend.neo4j_ontology_access.model.Directory} object with given directoryId.
   *
   * <p>This method can be used to determine, whether a repository is contained in a directory.
   *
   * @param repositoryId The repository's ID.
   * @param superDirectoryId The super directory's ID.
   * @return An {@link Optional<Repository>} containing the matching repository.
   */
  @Query(
      "MATCH (r:Repository { id: $repositoryId }) -[:BELONGS_TO*]-> (d:Directory { id: $superDirectoryId }) "
          + "RETURN r")
  Optional<Repository> findByIdAndSuperDirectoryId(
      @Param("repositoryId") String repositoryId,
      @Param("superDirectoryId") String superDirectoryId);

  Page<Repository> findByNameContaining(String name, Pageable pageable);

  @Query(
      "MATCH (r:Repository) -[:BELONGS_TO*]-> (:Directory { id: $superDirectoryId }) "
          + "WHERE r.name =~ '(?i).*' + $name + '.*' "
          + "RETURN r "
          + ":#{orderBy(#pageable)} SKIP $skip LIMIT $limit")
  Slice<Repository> findByNameContainingAndSuperDirectoryId(
      @Param("name") String name,
      @Param("superDirectoryId") String superDirectoryId,
      @Param("pageable") Pageable pageable);
}
