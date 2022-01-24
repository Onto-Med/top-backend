package care.smith.top.backend.neo4j_ontology_access.model;

public interface ClassRelationOwner {
  default OntologyVersion toOntologyVersion() {
    return (OntologyVersion) this;
  }

  default Repository toRepository() {
    return (Repository) this;
  }

  default boolean isOntologyVersion() {
    return this instanceof OntologyVersion;
  }

  default boolean isRepository() {
    return this instanceof Repository;
  }
}
