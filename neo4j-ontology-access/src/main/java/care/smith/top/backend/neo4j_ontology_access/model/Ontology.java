package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node
public class Ontology extends Directory {
  @Relationship(type = "BELONGS_TO")
  private Repository repository;

  @Relationship(type = "HAS_VERSION")
  private Set<OntologyVersion> versions;

  @Relationship(type = "CURRENT_VERSION")
  private OntologyVersion currentVersion;

  public Ontology createVersion(OntologyVersion version) {
    if (versions == null) versions = new HashSet<>();
    this.versions.add(version.setVersion(versions.size() + 1));
    return this;
  }

  public Repository getRepository() {
    return repository;
  }

  public Ontology setRepository(Repository repository) {
    this.repository = repository;
    return this;
  }

  public Set<OntologyVersion> getVersions() {
    return versions;
  }

  public Ontology setVersions(Set<OntologyVersion> versions) {
    this.versions = versions;
    return this;
  }

  public OntologyVersion getCurrentVersion() {
    return currentVersion;
  }

  public Ontology setCurrentVersion(OntologyVersion currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }
}
