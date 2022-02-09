package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
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

  /**
   * Search for current {@link ClassVersion} {@link Class}.
   *
   * @param classId The classId.
   * @return An {@link Optional<ClassVersion>} object containing the matching {@link ClassVersion}
   *     object with all related annotations.
   */
  @Query(
      "MATCH (c:Class { id: $classId }) -[cRel:CURRENT_VERSION]-> (cv:ClassVersion) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH a = (cv) -[:HAS_ANNOTATION*]-> (:Annotation) "
          + "RETURN cv, cRel, c, collect(nodes(a)), collect(relationships(a))")
  Optional<ClassVersion> findCurrentByClassId(@Param("classId") UUID classId);

  /**
   * Get all versions of a {@link Class}.
   *
   * @param classId The classId.
   * @return A {@link Slice<ClassVersion>} containing the versions.
   */
  @Query(
      "MATCH (cv:ClassVersion) -[cRel:IS_VERSION_OF]-> (c:Class { id: $classId }) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH a = (cv) -[:HAS_ANNOTATION*]-> (:Annotation) "
          + "RETURN cv, cRel, c, collect(nodes(a)), collect(relationships(a)) "
          + ":#{orderBy(#pageable)} SKIP $skip LIMIT $limit")
  Slice<ClassVersion> findByClassId(
      @Param("classId") UUID classId, @Param("pageable") Pageable pageable);

  // TODO: this query does not belong here because annotations are domain specific
  @Query(
      "MATCH (c:Class { repositoryId: $repositoryId }) -[cRel:CURRENT_VERSION]-> (cv:ClassVersion) "
          + "WHERE cv.hiddenAt IS NULL "
          + "OPTIONAL MATCH (cv) -[:HAS_ANNOTATION]-> (title:Annotation { property: 'title' }) "
          + "OPTIONAL MATCH (cv) -[:HAS_ANNOTATION]-> (type:Annotation { property: 'type' }) "
          + "OPTIONAL MATCH (cv) -[:HAS_ANNOTATION]-> (dataType:Annotation { property: 'dataType' }) "
          + "WITH c, cRel, cv, title, type, dataType "
          + "WHERE ($name IS NULL OR cv.name =~ '(?i).*' + $name + '.*' OR title.stringValue =~ '(?i).*' + $name + '.*') "
          + "AND ($type IS NULL OR type.stringValue = $type) "
          + "AND ($dataType IS NULL OR dataType.stringValue = $dataType) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH a = (cv) -[:HAS_ANNOTATION*]-> (:Annotation) "
          + "RETURN cv, cRel, c, collect(nodes(a)), collect(relationships(a)) "
          + ":#{orderBy(#pageable)} SKIP $skip LIMIT $limit")
  Slice<ClassVersion> findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
      @Param("repositoryId") String repositoryId,
      @Param("name") String name,
      @Param("type") String type,
      @Param("dataType") String dataType,
      @Param("pageable") Pageable pageable);

  @Query(
      "MATCH (:Class { id: $cls.__id__ }) -[:IS_SUBCLASS_OF { ownerId: $ownerId }]-> (c:Class) -[:CURRENT_VERSION]-> (cv:ClassVersion) "
          + "MATCH (cv) -[cRel:IS_VERSION_OF]-> (c) "
          + "WITH cv, collect(cRel) AS cRel, collect(c) AS c "
          + "OPTIONAL MATCH a = (cv) -[:HAS_ANNOTATION*]-> (:Annotation) "
          + "RETURN cv, cRel, c, collect(nodes(a)), collect(relationships(a))")
  Set<ClassVersion> getCurrentSuperClassVersionsByOwnerId(
      @Param("cls") Class cls, @Param("ownerId") String ownerId);

  @Query(
      "MATCH (current:ClassVersion) -[:IS_VERSION_OF]-> (c:Class)"
          + "WHERE id(current) = $classVersion.__id__ "
          + "MATCH (previous:ClassVersion) -[:IS_VERSION_OF]-> (c:Class) "
          + "WHERE previous.hiddenAt IS NULL AND id(current) <> id(previous) "
          + "MATCH p = shortestPath((current) -[:PREVIOUS_VERSION*]-> (previous)) "
          + "RETURN previous "
          + "ORDER BY length(p) LIMIT 1")
  Optional<ClassVersion> getPreviousUnhidden(@Param("classVersion") ClassVersion classVersion);
}
