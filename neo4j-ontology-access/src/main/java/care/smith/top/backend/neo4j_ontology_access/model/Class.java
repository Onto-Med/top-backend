package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Node
public class Class extends Annotatable {
  @Id private final UUID uuid;
  @Version private Long nodeVersion;
  @CreatedDate private Instant createdAt;
  @CreatedBy private User createdBy;

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

  /**
   * Add new version for this class. The version will get assigned a version number.
   *
   * @param version The new version.
   * @return This Class object.
   */
  public Class createVersion(ClassVersion version) {
    if (versions == null) versions = new HashSet<>();
    this.versions.add(version.setVersion(versions.size() + 1));
    return this;
  }

  /**
   * Get the specified version of this class. If parameter version is null, return the current
   * version.
   *
   * @param version The requested version number.
   * @return A {@link ClassVersion} object for this Class object.
   */
  public Optional<ClassVersion> getVersion(Integer version) {
    if (version == null) {
      return getCurrentVersion();
    } else {
      return getVersions().stream().filter(v -> v.getVersion() == version).findFirst();
    }
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

  public Optional<ClassVersion> getCurrentVersion() {
    return Optional.ofNullable(currentVersion);
  }

  public Class setCurrentVersion(ClassVersion currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }

  public Long getNodeVersion() {
    return nodeVersion;
  }
}
