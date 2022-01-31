package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassVersionRepository extends PagingAndSortingRepository<ClassVersion, Long> {

  /**
   * Search for a {@link ClassVersion} by classId and version number and add all annotations to the
   * result.
   *
   * @param classId The classId.
   * @param version The version number.
   * @return An {@link Optional<ClassVersion>} object containing the matching {@link ClassVersion}
   *     object with all related annotations.
   */
  @Query(
      "MATCH (cv:ClassVersion { version: $version }) -[cRel:IS_VERSION_OF]-> (c:Class { id: $classId }) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH a = (cv) -[:HAS_ANNOTATION*]-> (:Annotation) "
          + "RETURN cv, cRel, c, collect(nodes(a)), collect(relationships(a))")
  Optional<ClassVersion> findByClassIdAndVersion(
      @Param("classId") UUID classId, @Param("version") Integer version);
}
