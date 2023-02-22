package care.smith.top.backend.model;

import care.smith.top.backend.model.key.OrganisationMembershipKeyDao;
import care.smith.top.model.OrganisationMembership;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity(name = "organisation_membership")
public class OrganisationMembershipDao {
  @EmbeddedId private OrganisationMembershipKeyDao id;

  @ManyToOne(optional = false)
  @MapsId("userId")
  private UserDao user;

  @ManyToOne(optional = false)
  @MapsId("organisationId")
  private OrganisationDao organisation;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Permission permission = Permission.READ;

  public OrganisationMembershipDao() {}

  public OrganisationMembershipDao(@NotNull UserDao user, @NotNull OrganisationDao organisation) {
    this.id = new OrganisationMembershipKeyDao(user.getId(), organisation.getId());
    this.user = user;
    this.organisation = organisation;
  }

  public OrganisationMembershipDao(
      @NotNull UserDao user,
      @NotNull OrganisationDao organisation,
      @NotNull Permission permission) {
    this(user, organisation);
    this.permission = permission;
  }

  public OrganisationMembershipDao user(@NotNull UserDao user) {
    this.user = user;
    return this;
  }

  public OrganisationMembershipDao organisation(@NotNull OrganisationDao organisation) {
    this.organisation = organisation;
    return this;
  }

  public OrganisationMembershipDao permission(@NotNull Permission permission) {
    this.permission = permission;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrganisationMembershipDao that = (OrganisationMembershipDao) o;

    if (!getUser().equals(that.getUser())) return false;
    if (!getOrganisation().equals(that.getOrganisation())) return false;
    return getPermission() == that.getPermission();
  }

  @Override
  public int hashCode() {
    int result = getUser().hashCode();
    result = 31 * result + getOrganisation().hashCode();
    result = 31 * result + getPermission().hashCode();
    return result;
  }

  public OrganisationMembership toApiModel() {
    return new OrganisationMembership()
        .organisation(getOrganisation().toApiModel())
        .user(getUser().toApiModel())
        .permission(care.smith.top.model.Permission.fromValue(getPermission().name()));
  }

  public UserDao getUser() {
    return user;
  }

  public OrganisationDao getOrganisation() {
    return organisation;
  }

  public Permission getPermission() {
    return permission;
  }
}
