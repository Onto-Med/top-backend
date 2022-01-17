package care.smith.top.backend.resource.service;

import care.smith.top.backend.neo4j_ontology_access.Class;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface OntologyRepository extends Neo4jRepository<Class, Long> {
  Class findClassByName(String name);
}
