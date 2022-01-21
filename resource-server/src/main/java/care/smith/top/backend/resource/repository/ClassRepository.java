package care.smith.top.backend.resource.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface ClassRepository extends Neo4jRepository<Class, UUID> {
  Class findClassByUuid(UUID id);
}
