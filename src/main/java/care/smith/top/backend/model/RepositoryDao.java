package care.smith.top.backend.model;

import care.smith.top.model.Organisation;
import care.smith.top.model.Repository;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

@Entity(name = "repository")
@EntityListeners(AuditingEntityListener.class)
public class RepositoryDao {
  @Id private String id;
  private String name;
  private String description;

  @Column(name = "is_primary")
  private Boolean primary = false;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate private OffsetDateTime updatedAt;
  @ManyToOne private Organisation organisation;

  @OneToMany(mappedBy = "repository")
  private List<EntityDao> entities = null;

  public RepositoryDao() {}

  public RepositoryDao(String id, String name, String description, Boolean primary) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.primary = primary;
  }

  public RepositoryDao(Repository repository) {
    id = repository.getId();
    name = repository.getName();
    description = repository.getDescription();
    primary = repository.isPrimary();
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

  public RepositoryDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public RepositoryDao updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public RepositoryDao organisation(Organisation organisation) {
    this.organisation = organisation;
    return this;
  }

  public RepositoryDao entities(List<EntityDao> entities) {
    this.entities = entities;
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
    result = 31 * result + (getOrganisation() != null ? getOrganisation().hashCode() : 0);
    return result;
  }

  @NotNull
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Organisation getOrganisation() {
    return organisation;
  }

  public List<EntityDao> getEntities() {
    return entities;
  }
}
