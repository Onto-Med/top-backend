package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.neo4j.cypherdsl.core.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ClassVersionRepository
    extends PagingAndSortingRepository<ClassVersion, Long>,
        CypherdslConditionExecutor<ClassVersion>,
        CypherdslStatementExecutor<ClassVersion> {

  default Iterable<ClassVersion> findAllEquivalentVersions(@Param("fork") ClassVersion fork) {
    Node forkNode = Cypher.node("ClassVersion").named("fork");
    Node originVersion = Cypher.node("ClassVersion").named("originVersion");
    Node originClass = Cypher.node("Class").named("originClass");
    Relationship equivalentRel =
        forkNode.relationshipTo(originVersion, "IS_EQUIVALENT_TO").named("equivalentRel");
    Relationship cRel = originVersion.relationshipTo(originClass, "IS_VERSION_OF").named("cRel");

    StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere query =
        Cypher.match(forkNode)
            .where(forkNode.internalId().isEqualTo(Cypher.anonParameter(fork.getId())))
            .match(equivalentRel)
            .match(cRel)
            .with(originClass, cRel, originVersion);

    return findAll(query.returning(originClass, cRel, originVersion).build());
  }

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

  /**
   * Update the IS_EQUIVALENT_TO relationship of a {@link ClassVersion} node.
   *
   * @param fork The start node of the relationship.
   * @param origin The end node of the relationship. If null, the relationship will be dropped.
   * @return The {@code ClassVersion} node of the fork.
   */
  default Optional<ClassVersion> setEquivalentVersion(
      @NonNull ClassVersion fork, @Nullable ClassVersion origin) {
    Node forkNode = Cypher.node("ClassVersion").named("fork");
    Relationship oldRel =
        forkNode.relationshipTo(Cypher.node("ClassVersion"), "IS_EQUIVALENT_TO").named("oldRel");

    StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere query =
        Cypher.match(forkNode)
            .where(forkNode.internalId().isEqualTo(Cypher.anonParameter(fork.getId())))
            .optionalMatch(oldRel)
            .delete(oldRel)
            .with(forkNode);

    if (origin != null) {
      Node originNode = Cypher.node("ClassVersion").named("origin");
      Relationship newRel = forkNode.relationshipTo(originNode, "IS_EQUIVALENT_TO").named("newRel");
      query =
          query
              .match(originNode)
              .where(originNode.internalId().isEqualTo(Cypher.anonParameter(origin.getId())))
              .create(newRel)
              .with(forkNode);
    }

    return findOne(query.returning(forkNode).build());
  }

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
