package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2)) ")
  Optional<ClassVersion> findByClassIdAndVersion(
      @Param("classId") String classId, @Param("version") Integer version);

  /**
   * Search for current {@link ClassVersion} {@link Class}.
   *
   * @param classId The classId.
   * @return An {@link Optional<ClassVersion>} object containing the matching {@link ClassVersion}
   *     object with all related annotations.
   */
  @Query(
      "MATCH (c:Class { id: $classId }) -[:CURRENT_VERSION]-> (cv:ClassVersion) "
          + "MATCH (cv) -[cRel:IS_VERSION_OF]-> (c) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2)) ")
  Optional<ClassVersion> findCurrentByClassId(@Param("classId") String classId);

  /**
   * Get a page of versions of a {@link Class}.
   *
   * @param classId The classId.
   * @param pageable The page request.
   * @return A {@link Slice<ClassVersion>} containing the versions.
   */
  @Query(
      "MATCH (cv:ClassVersion) -[cRel:IS_VERSION_OF]-> (c:Class { id: $classId }) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2)) "
          + ":#{orderBy(#pageable)} SKIP $skip LIMIT $limit")
  Slice<ClassVersion> findByClassId(
      @Param("classId") String classId, @Param("pageable") Pageable pageable);

  /**
   * Get all versions of a {@link Class}.
   *
   * @param classId The classId.
   * @return A {@link Set<ClassVersion>} containing the versions.
   */
  @Query(
      "MATCH (cv:ClassVersion) -[cRel:IS_VERSION_OF]-> (c:Class { id: $classId }) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2))")
  Set<ClassVersion> findAllByClassId(@Param("classId") String classId);

  // TODO: this query does not belong here because annotations are domain specific
  @Query(
      "MATCH (c:Class) -[:CURRENT_VERSION]-> (cv:ClassVersion) "
          + "WHERE ($repositoryId IS NULL OR c.repositoryId = $repositoryId) "
          + "AND ($type IS NULL OR ANY(l in labels(c) WHERE l IN $type)) "
          + "MATCH (cv) -[cRel:IS_VERSION_OF]-> (c) "
          + "OPTIONAL MATCH (cv) -[:HAS_ANNOTATION]-> (title:Annotation { property: 'title' }) "
          + "OPTIONAL MATCH (cv) -[:HAS_ANNOTATION]-> (dataType:Annotation { property: 'dataType' }) "
          + "WITH c, cRel, cv, title, dataType "
          + "WHERE ($name IS NULL OR cv.name =~ '(?i).*' + $name + '.*' OR title.stringValue =~ '(?i).*' + $name + '.*') "
          + "AND ($dataType IS NULL OR dataType.stringValue = $dataType) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2)) "
          + ":#{orderBy(#pageable)} SKIP $skip LIMIT $limit")
  Slice<ClassVersion> findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
      @Param("repositoryId") String repositoryId,
      @Param("name") String name,
      @Param("type") List<String> type,
      @Param("dataType") String dataType,
      @Param("pageable") Pageable pageable);

  @Query(
      "MATCH (c:Class { id: $classId }) <-[cRel:IS_VERSION_OF]- (cv:ClassVersion) "
          + "WITH cv, collect(cRel) as cRel, collect(c) as c "
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2)) "
          + "ORDER BY cv.version DESC LIMIT 1 ")
  Optional<ClassVersion> findLatestByClassId(@Param("classId") String classId);

  @Query(
      "MATCH (:Class { id: $cls.__id__ }) -[:IS_SUBCLASS_OF { ownerId: $ownerId }]-> (c:Class) -[:CURRENT_VERSION]-> (cv:ClassVersion) "
          + "MATCH (cv) -[cRel:IS_VERSION_OF]-> (c) "
          + "WITH cv, collect(cRel) AS cRel, collect(c) AS c "
          + "OPTIONAL MATCH p = (cv) -[:HAS_ANNOTATION*]-> (a:Annotation) "
          + "OPTIONAL MATCH p2 = (a:Annotation) -[:HAS_CLASS_VALUE]-> (:Class) "
          + "RETURN cv, cRel, c, collect(nodes(p)), collect(relationships(p)), collect(nodes(p2)), collect(relationships(p2)) ")
  Set<ClassVersion> getCurrentSuperClassVersionsByOwnerId(
      @Param("cls") Class cls, @Param("ownerId") String ownerId);

  @Query(
      "MATCH (current:ClassVersion) -[:IS_VERSION_OF]-> (c:Class)"
          + "WHERE id(current) = $classVersion.__id__ "
          + "MATCH (current) <-[:PREVIOUS_VERSION]- (next:ClassVersion) "
          + "RETURN next ")
  Optional<ClassVersion> getNext(@Param("classVersion") ClassVersion classVersion);

  @Query(
      "MATCH (current:ClassVersion) -[:IS_VERSION_OF]-> (c:Class)"
          + "WHERE id(current) = $classVersion.__id__ "
          + "MATCH (current) -[:PREVIOUS_VERSION]-> (previous:ClassVersion) "
          + "RETURN previous ")
  Optional<ClassVersion> getPrevious(@Param("classVersion") ClassVersion classVersion);

  @Query(
      "MATCH (prev:ClassVersion) "
          + "WHERE id(prev) = $previousClassVersion.__id__ "
          + "MATCH (c:ClassVersion) "
          + "WHERE id(c) = $classVersion.__id__ "
          + "CREATE (c) -[:PREVIOUS_VERSION]-> (prev) ")
  void setPreviousVersion(
      @Param("classVersion") ClassVersion classVersion,
      @Param("previousClassVersion") ClassVersion previousClassVersion);
}
