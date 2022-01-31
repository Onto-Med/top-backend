package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@org.springframework.stereotype.Repository
public interface ClassRepository extends PagingAndSortingRepository<Class, UUID> {
  @Query(
      "MATCH (super:Class {id: $cls.__id__}) <-[:HAS_SUPERCLASS]- (cr:ClassRelation { ownerId: $repository.__id__ }) "
          + "MATCH (cr) <-[:IS_SUBCLASS_OF]- (sub:Class) "
          + "RETURN sub ORDER BY sub.index")
  Stream<Class> findSubclasses(@Param("cls") Class cls, @Param("repository") Repository repository);

  @Query(
    "MATCH (c:Class { repositoryId: $repository.__id__ }) "
      + "WHERE NOT (c) -[:IS_SUBCLASS_OF]-> () "
      + "RETURN c")
  Set<Class> findRootClassesByRepository(@Param("repository") Repository repository);
}
