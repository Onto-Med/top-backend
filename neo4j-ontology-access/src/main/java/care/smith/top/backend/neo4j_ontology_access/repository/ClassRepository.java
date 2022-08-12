package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@org.springframework.stereotype.Repository
public interface ClassRepository
    extends PagingAndSortingRepository<Class, String>,
        CypherdslConditionExecutor<Class>,
        CypherdslStatementExecutor<Class> {
  default Optional<Class> findOrigin(Class fork) {
    Node forkNode =
        Cypher.node("Class").named("fork").withProperties("id", Cypher.anonParameter(fork.getId()));
    Node originNode = Cypher.node("Class").named("origin");

    return findOne(
        Cypher.match(forkNode.relationshipTo(originNode, "IS_FORK_OF"))
            .returning(originNode)
            .build());
  }

  @Query(
      "MATCH (super:Class {id: $classId}) <-[:IS_SUBCLASS_OF { ownerId: $repositoryId }]- (sub:Class) "
          + "RETURN sub ORDER BY sub.index")
  Stream<Class> findSubclasses(
      @Param("classId") String id, @Param("repositoryId") String repositoryId);

  @Query(
      "MATCH (c:Class { repositoryId: $repository.__id__ }) "
          + "OPTIONAL MATCH (c) -[:IS_SUBCLASS_OF]-> (super) "
          + "WITH c, collect(super) as super "
          + "WHERE isEmpty(super) "
          + "RETURN c")
  Set<Class> findRootClassesByRepository(@Param("repository") Repository repository);

  Optional<Class> findByIdAndRepositoryId(String id, String repositoryId);

  default boolean forkExists(String entityId, String repositoryId) {
    Node cls = Cypher.node("Class");
    Node origin = cls.withProperties("id", Cypher.anonParameter(entityId)).named("origin");
    Node fork = cls.withProperties("repositoryId", Cypher.anonParameter(repositoryId)).named("fork");
    Relationship forkRel = fork.relationshipTo(origin, "IS_FORK_OF").unbounded();

    return exists(Cypher.match(forkRel).asCondition());
  }

  default Collection<Class> getForks(String classId) {
    Node origin =
        Cypher.node("Class").withProperties("id", Cypher.anonParameter(classId)).named("origin");
    Node fork = Cypher.node("Class").named("c");
    Relationship forkRel = fork.relationshipTo(origin, "IS_FORK_OF").named("forkRel");

    return this.findAll(Cypher.match(forkRel).returning(fork).build());
  }

  default Optional<Class> getFork(String classId, String repositoryId) {
    Node origin =
        Cypher.node("Class").withProperties("id", Cypher.anonParameter(classId)).named("origin");
    Node fork =
        Cypher.node("Class")
            .withProperties("repositoryId", Cypher.anonParameter(repositoryId))
            .named("c");
    Relationship forkRel = fork.relationshipTo(origin, "IS_FORK_OF").named("forkRel");

    return this.findOne(Cypher.match(forkRel).returning(fork).build());
  }

  /**
   * Get a new version number for the given {@link Class} object. The returned number is the highest
   * available version number of this class incremented by 1. If the class does not exist or has no
   * version, this method returns 1.
   *
   * @param cls The {@link Class} object.
   * @return A version number to be used for a new {@link ClassVersion}.
   */
  @Query(
      "OPTIONAL MATCH (c:Class { id: $cls.__id__ }) "
          + "OPTIONAL MATCH (c) <-[:IS_VERSION_OF]- (cv:ClassVersion) "
          + "WITH max(cv.version) AS version "
          + "RETURN CASE version WHEN NULL THEN 0 ELSE version END + 1")
  Integer getNextVersion(@Param("cls") Class cls);

  @Query(
      "MATCH (c:Class { id: $cls.__id__ }) "
          + "MATCH (cv:ClassVersion) "
          + "WHERE id(cv) = $classVersion.__id__ "
          + "OPTIONAL MATCH (c) -[rel:CURRENT_VERSION]-> (:ClassVersion) "
          + "DELETE rel "
          + "CREATE (c) -[:CURRENT_VERSION]-> (cv) ")
  void setCurrent(@Param("cls") Class cls, @Param("classVersion") ClassVersion classVersion);

  @Query(
      "MATCH (fork:Class { id: $forkId }) "
          + "MATCH (origin:Class { id: $originId }) "
          + "WHERE fork.repositoryId <> origin.repositoryId "
          + "OPTIONAL MATCH (fork) -[rel:IS_FORK_OF]-> (:Class) "
          + "DELETE rel "
          + "CREATE (fork) -[:IS_FORK_OF]-> (origin) ")
  void setFork(@Param("forkId") String forkId, @Param("originId") String originId);
}
