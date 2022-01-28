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

  @Relationship(type = "IS_SUBCLASS_OF")
  private ClassRelation superClassRelation;

  @PersistenceConstructor
  public Class(UUID uuid) {
    this.uuid = uuid;
  }

  /**
   * Add new version for this class. The version will get assigned a version number and the UUID of this class.
   *
   * @param classVersion The new version.
   * @param setCurrent If true, given {@link ClassVersion} will be set to the current version of
   *     this {@link Class} object.
   * @return This {@link Class} object.
   */
  public Class createVersion(ClassVersion classVersion, boolean setCurrent) {
    if (versions == null) versions = new HashSet<>();

    int version = 1;
    if (getCurrentVersion().isPresent()) version = getCurrentVersion().get().getVersion() + 1;

    versions.add(classVersion.setVersion(version).setClassId(getUuid()));
    if (setCurrent) setCurrentVersion(classVersion);

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
    if (!versions.contains(currentVersion))
      throw new UnsupportedOperationException(
          String.format("%s does not belong to '%s'!", currentVersion.getClass().getName(), uuid));
    this.currentVersion = currentVersion;
    return this;
  }

  public Long getNodeVersion() {
    return nodeVersion;
  }

  public ClassRelation getSuperClassRelation() {
    return superClassRelation;
  }

  public Class setSuperClassRelation(ClassRelation superClassRelation) {
    this.superClassRelation = superClassRelation;
    return this;
  }
}
