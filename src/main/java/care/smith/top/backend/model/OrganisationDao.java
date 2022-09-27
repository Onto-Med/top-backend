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
import java.util.UUID;

@Entity(name = "organisation")
@EntityListeners(AuditingEntityListener.class)
public class OrganisationDao {
  @Id private String id;
  private String name;
  private String description;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate private OffsetDateTime updatedAt;
  @ManyToOne private Organisation superOrganisation;

  @OneToMany(mappedBy = "superOrganisation")
  private List<OrganisationDao> subOrganisations = null;

  @OneToMany(mappedBy = "organisation")
  private List<RepositoryDao> repositories = null;

  public OrganisationDao() {}

  public OrganisationDao(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public OrganisationDao(Organisation organisation) {
    id = organisation.getId() == null ? UUID.randomUUID().toString() : organisation.getId();
    name = organisation.getName();
    description = organisation.getDescription();
  }

  public OrganisationDao id(@NotNull String id) {
    this.id = id;
    return this;
  }

  public OrganisationDao name(String name) {
    this.name = name;
    return this;
  }

  public OrganisationDao description(String description) {
    this.description = description;
    return this;
  }

  public OrganisationDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public OrganisationDao updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public OrganisationDao superOrganisation(OrganisationDao superOrganisation) {
    this.superOrganisation = superOrganisation;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrganisationDao that = (OrganisationDao) o;

    if (!getId().equals(that.getId())) return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null)
      return false;
    if (getDescription() != null
        ? !getDescription().equals(that.getDescription())
        : that.getDescription() != null) return false;
    return getSuperOrganisation() != null
        ? getSuperOrganisation().equals(that.getSuperOrganisation())
        : that.getSuperOrganisation() == null;
  }

  @Override
  public int hashCode() {
    int result = getId().hashCode();
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (getSuperOrganisation() != null ? getSuperOrganisation().hashCode() : 0);
    return result;
  }

  public OrganisationDao subOrganisations(List<Organisation> subOrganisations) {
    this.subOrganisations = subOrganisations;
    return this;
  }

  public OrganisationDao repositories(List<Repository> repositories) {
    this.repositories = repositories;
    return this;
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Organisation getSuperOrganisation() {
    return superOrganisation;
  }

  public List<Organisation> getSubOrganisations() {
    return subOrganisations;
  }

  public List<Repository> getRepositories() {
    return repositories;
  }
}
