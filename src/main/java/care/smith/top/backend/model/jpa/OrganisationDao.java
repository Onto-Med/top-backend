package care.smith.top.backend.model.jpa;

import care.smith.top.model.Organisation;
import java.time.OffsetDateTime;
import java.util.*;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity(name = "organisation")
@EntityListeners(AuditingEntityListener.class)
public class OrganisationDao {
  @Id private String id;
  private String name;

  @Column(length = 5000)
  private String description;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate private OffsetDateTime updatedAt;
  @ManyToOne private OrganisationDao superOrganisation;

  @OneToMany(mappedBy = "superOrganisation")
  private List<OrganisationDao> subOrganisations = null;

  @OneToMany(mappedBy = "organisation", cascade = CascadeType.REMOVE)
  private List<RepositoryDao> repositories = null;

  @OneToMany(mappedBy = "organisation", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<OrganisationMembershipDao> members = new ArrayList<>();

  @OneToMany(mappedBy = "organisation", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<OrganisationDataSourceDao> dataSources = new ArrayList<>();

  public OrganisationDao() {}

  public OrganisationDao(@NotNull String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public OrganisationDao(@NotNull Organisation organisation) {
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

  public OrganisationDao members(Collection<OrganisationMembershipDao> members) {
    this.members = members;
    return this;
  }

  public OrganisationDao dataSources(Collection<OrganisationDataSourceDao> dataSources) {
    this.dataSources = dataSources;
    return this;
  }

  public OrganisationDao addDataSource(OrganisationDataSourceDao dataSource) {
    // TODO: update existing entry and hash
    this.dataSources.add(dataSource);
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

  public OrganisationDao subOrganisations(List<OrganisationDao> subOrganisations) {
    this.subOrganisations = subOrganisations;
    return this;
  }

  public OrganisationDao repositories(List<RepositoryDao> repositories) {
    this.repositories = repositories;
    return this;
  }

  public Organisation toApiModel(UserDao user) {
    Organisation organisation = toApiModel();
    if (user == null || user.isAdmin()) {
      organisation.setPermission(care.smith.top.model.Permission.MANAGE);
    } else {
      Optional<OrganisationMembershipDao> membership =
          getMembers().stream().filter(m -> m.getUser().getId().equals(user.getId())).findFirst();
      membership.ifPresent(
          organisationMembershipDao ->
              organisation.setPermission(
                  Permission.toApiModel(organisationMembershipDao.getPermission())));
    }
    return organisation;
  }

  public Organisation toApiModel() {
    Organisation organisation =
        new Organisation()
            .id(id)
            .name(name)
            .description(description)
            .createdAt(createdAt)
            .updatedAt(updatedAt);
    if (superOrganisation != null)
      organisation.setSuperOrganisation(
          new Organisation().id(superOrganisation.getId()).name(superOrganisation.getName()));
    return organisation;
  }

  public OrganisationDao update(Organisation data) {
    return name(data.getName()).description(data.getDescription());
  }

  public OrganisationMembershipDao setMemberPermission(
      @NotNull UserDao user, @NotNull Permission permission) {
    Optional<OrganisationMembershipDao> member =
        members.stream().filter(m -> m.getUser().getId().equals(user.getId())).findFirst();
    if (member.isPresent()) {
      member.get().permission(permission);
      return member.get();
    } else {
      OrganisationMembershipDao membership = new OrganisationMembershipDao(user, this, permission);
      members.add(membership);
      return membership;
    }
  }

  public boolean removeDataSourceById(String dataSourceId) {
    return dataSources.removeIf(ds -> ds.getDataSourceId().equals(dataSourceId));
  }

  public boolean hasDataSource(String dataSource) {
    return dataSources.stream().anyMatch(ds -> ds.getDataSourceId().equals(dataSource));
  }

  public Collection<OrganisationMembershipDao> getMembers() {
    return members;
  }

  public Collection<OrganisationDataSourceDao> getDataSources() {
    return dataSources;
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OrganisationDao getSuperOrganisation() {
    return superOrganisation;
  }

  public List<OrganisationDao> getSubOrganisations() {
    return subOrganisations;
  }

  public List<RepositoryDao> getRepositories() {
    return repositories;
  }
}
