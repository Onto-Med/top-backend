package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import care.smith.top.backend.neo4j_ontology_access.model.Expression;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AnnotationRepository
    extends PagingAndSortingRepository<Annotation, Long>,
        CypherdslConditionExecutor<Annotation>,
        CypherdslStatementExecutor<Annotation> {
  @Query(
      "MATCH p = (a:Annotation { property: $property }) -[:HAS_CLASS_VALUE]-> (:Class { id: $cls.__id__ }) "
          + "RETURN a, collect(nodes(p)), collect(relationships(p)) ")
  Iterable<? extends Annotation> findAllByClassValueAndProperty(
      @Param("cls") Class cls, @Param("property") String property);

  @Query(
      "MATCH (cv:ClassVersion) -[:HAS_ANNOTATION]-> (a:Annotation) "
          + "WHERE id(cv) = $classVersion.__id__ "
          + "AND a.property = $property "
          + "OPTIONAL MATCH (a) -[cRel:HAS_CLASS_VALUE]-> (c:Class) "
          + "RETURN a, cRel, c")
  Set<Annotation> findByClassVersionAndProperty(
      @Param("classVersion") ClassVersion classVersion, @Param("property") String property);
}
