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
  @Id private final UUID id;
  @Version private Long nodeVersion;
  @CreatedDate private Instant createdAt;
  @CreatedBy private User createdBy;

  @Relationship(type = "IS_FORK_OF")
  private Class forkedClass;

  @Relationship(type = "CURRENT_VERSION")
  private ClassVersion currentVersion;

  @Relationship(type = "IS_SUBCLASS_OF")
  private Set<ClassRelation> superClassRelations;

  private String repositoryId;

  @PersistenceConstructor
  public Class(UUID id) {
    this.id = id;
  }

  public Class() {
    this(UUID.randomUUID());
  }

  public Class addSuperClassRelation(ClassRelation superClassRelation) {
    if (superClassRelations == null) superClassRelations = new HashSet<>();
    superClassRelations.add(superClassRelation);
    return this;
  }

  public Class addSuperClass(Class superClass, String ownerId, Integer index) {
    addSuperClassRelation(
        new ClassRelation().setSuperclass(superClass).setOwnerId(ownerId).setIndex(index));
    return this;
  }

  public UUID getId() {
    return id;
  }

  public Class getForkedClass() {
    return forkedClass;
  }

  public Class setForkedClass(Class forkedClass) {
    this.forkedClass = forkedClass;
    return this;
  }

  public Optional<ClassVersion> getCurrentVersion() {
    return Optional.ofNullable(currentVersion);
  }

  public Class setCurrentVersion(ClassVersion currentVersion) {
    currentVersion.setaClass(this);
    if (this.currentVersion != null) currentVersion.setPreviousVersion(this.currentVersion);
    this.currentVersion = currentVersion;
    return this;
  }

  public Long getNodeVersion() {
    return nodeVersion;
  }

  public Set<ClassRelation> getSuperClassRelations() {
    return superClassRelations;
  }

  public Class setSuperClassRelations(Set<ClassRelation> superClassRelations) {
    this.superClassRelations = superClassRelations;
    return this;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public Class setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
    return this;
  }
}
