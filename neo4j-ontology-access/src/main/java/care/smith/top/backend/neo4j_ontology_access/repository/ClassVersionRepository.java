package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface ClassVersionRepository extends PagingAndSortingRepository<ClassVersion, Long> {
  // TODO: implement this method
  Set<Annotation> findAnnotationsByProperty(ClassVersion classVersion, String property);
}
