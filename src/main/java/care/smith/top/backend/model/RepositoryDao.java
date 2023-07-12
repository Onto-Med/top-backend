package care.smith.top.backend.model;

import care.smith.top.model.Organisation;
import care.smith.top.model.Repository;
import care.smith.top.model.RepositoryType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity(name = "repository")
@EntityListeners(AuditingEntityListener.class)
public class RepositoryDao {
  @Id private String id;
  private String name;

  @Column(length = 5000)
  private String description;

  @Column(name = "is_primary")
  private Boolean primary = false;

  private RepositoryType repositoryType;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate private OffsetDateTime updatedAt;
  @ManyToOne private OrganisationDao organisation;

  @OneToMany(mappedBy = "repository", cascade = CascadeType.REMOVE)
  private List<EntityDao> entities = null;

  @OneToMany(mappedBy = "repository", cascade = CascadeType.REMOVE)
  private List<QueryDao> queries = null;

  public RepositoryDao() {}

  public RepositoryDao(
      String id, String name, String description, Boolean primary, RepositoryType repositoryType) {
    this.id = id == null ? UUID.randomUUID().toString() : id;
    this.name = name;
    this.description = description;
    this.primary = primary;
    this.repositoryType = repositoryType;
  }

  public RepositoryDao(@NotNull Repository repository) {
    id = repository.getId();
    name = repository.getName();
    description = repository.getDescription();
    primary = repository.isPrimary();
    repositoryType = repository.getRepositoryType();
  }

  public RepositoryDao id(@NotNull String id) {
    this.id = id;
    return this;
  }

  public RepositoryDao name(String name) {
    this.name = name;
    return this;
  }

  public RepositoryDao description(String description) {
    this.description = description;
    return this;
  }

  public RepositoryDao primary(Boolean primary) {
    this.primary = primary;
    return this;
  }

  public RepositoryDao repositoryType(RepositoryType repositoryType) {
    this.repositoryType = repositoryType;
    return this;
  }

  public RepositoryDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public RepositoryDao updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public RepositoryDao organisation(OrganisationDao organisation) {
    this.organisation = organisation;
    return this;
  }

  public RepositoryDao entities(List<EntityDao> entities) {
    this.entities = entities;
    return this;
  }

  public RepositoryDao queries(List<QueryDao> queries) {
    this.queries = queries;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepositoryDao that = (RepositoryDao) o;

    if (!getId().equals(that.getId())) return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null)
      return false;
    if (getDescription() != null
        ? !getDescription().equals(that.getDescription())
        : that.getDescription() != null) return false;
    if (getPrimary() != null ? !getPrimary().equals(that.getPrimary()) : that.getPrimary() != null)
      return false;
    if (getRepositoryType() != null
        ? !getRepositoryType().equals(that.getRepositoryType())
        : that.getRepositoryType() != null) return false;
    return getOrganisation() != null
        ? getOrganisation().equals(that.getOrganisation())
        : that.getOrganisation() == null;
  }

  @Override
  public int hashCode() {
    int result = getId().hashCode();
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (getPrimary() != null ? getPrimary().hashCode() : 0);
    result = 31 * result + (getRepositoryType() != null ? getRepositoryType().hashCode() : 0);
    result = 31 * result + (getOrganisation() != null ? getOrganisation().hashCode() : 0);
    return result;
  }

  public Repository toApiModel() {
    Repository repository =
        new Repository()
            .id(id)
            .name(name)
            .description(description)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .primary(primary)
            .repositoryType(repositoryType);
    if (organisation != null)
      repository.organisation(
          new Organisation().id(organisation.getId()).name(organisation.getName()));
    return repository;
  }

  public RepositoryDao update(Repository data) {
    // TODO: if (user is admin) update primary
    return name(data.getName()).description(data.getDescription()).primary(data.isPrimary());
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public RepositoryType getRepositoryType() {
    return repositoryType;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OrganisationDao getOrganisation() {
    return organisation;
  }

  public List<EntityDao> getEntities() {
    return entities;
  }

  public List<QueryDao> getQueries() {
    return queries;
  }

  public String getDisplayName() {
    if (getName() != null) return getName();
    return getId();
  }
}
