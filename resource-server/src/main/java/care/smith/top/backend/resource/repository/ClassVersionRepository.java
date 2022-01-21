package care.smith.top.backend.resource.repository;

import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.repository.CrudRepository;

public interface ClassVersionRepository extends CrudRepository<ClassVersion, Long> {
}
