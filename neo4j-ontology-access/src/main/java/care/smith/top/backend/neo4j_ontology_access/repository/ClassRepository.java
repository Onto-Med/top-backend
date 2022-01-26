package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;
import java.util.stream.Stream;

@org.springframework.stereotype.Repository
public interface ClassRepository extends PagingAndSortingRepository<Class, UUID> {
  @Query(
      "MATCH (super:Class {uuid: $cls.uuid}) <-[:HAS_SUPERCLASS]- (cr:ClassRelation {id: $repository.id}) "
          + "MATCH (cr) <-[:IS_SUBCLASS_OF]- (sub:Class) "
          + "RETURN sub ORDER BY index")
  Stream<Class> findSubclasses(Class cls, Repository repository);
}
