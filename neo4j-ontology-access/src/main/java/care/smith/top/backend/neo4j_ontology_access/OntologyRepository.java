package care.smith.top.backend.neo4j_ontology_access;

import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface OntologyRepository extends Neo4jRepository<ClassVersion, UUID> {
  ClassVersion findClassById(UUID id);
}
