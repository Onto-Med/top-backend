package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AnnotationRepository extends PagingAndSortingRepository<Annotation, Long> {
  @Query(
    "MATCH (cv:ClassVersion) -[:HAS_ANNOTATION]-> (a:Annotation) "
      + "WHERE id(cv) = $classVersion.__id__ "
      + "AND a.property = $property "
      + "RETURN a")
  Set<Annotation> findByClassVersionAndProperty(
    @Param("classVersion") ClassVersion classVersion, @Param("property") String property);
}
