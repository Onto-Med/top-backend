package care.smith.top.backend.neo4j_ontology_access.model;

import java.util.Set;

public interface ClassRelationOwner {
  default OntologyVersion toOntologyVersion() {
    return (OntologyVersion) this;
  }

  default Repository toRepository() {
    return (Repository) this;
  }

  ClassRelationOwner addRootClass(RootClass rootClass);

  default boolean isOntologyVersion() {
    return this instanceof OntologyVersion;
  }

  default boolean isRepository() {
    return this instanceof Repository;
  }

  Set<RootClass> getRootClasses();

  ClassRelationOwner setRootClasses(Set<RootClass> rootClasses);
}
