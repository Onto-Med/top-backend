package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Node
public class Class extends Annotatable {
  @Id private final UUID uuid;

  @Relationship(type = "IS_FORK_OF")
  private Class forkedClass;

  @Relationship(type = "HAS_VERSION")
  private Set<ClassVersion> versions;

  @Relationship(type = "CURRENT_VERSION")
  private ClassVersion currentVersion;

  @PersistenceConstructor
  public Class(UUID uuid) {
    this.uuid = uuid;
  }

  public Class createVersion(ClassVersion version) {
    if (versions == null) versions = new HashSet<>();
    this.versions.add(version);
    return this;
  }

  public UUID getUuid() {
    return uuid;
  }

  public Class getForkedClass() {
    return forkedClass;
  }

  public Class setForkedClass(Class forkedClass) {
    this.forkedClass = forkedClass;
    return this;
  }

  public Set<ClassVersion> getVersions() {
    return versions;
  }

  public ClassVersion getCurrentVersion() {
    return currentVersion;
  }

  public Class setCurrentVersion(ClassVersion currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }
}
