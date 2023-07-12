package care.smith.top.backend.model.key;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class OrganisationMembershipKeyDao implements Serializable {
  private static final long serialVersionUID = 1037838649571046646L;

  @Column(nullable = false)
  String userId;

  @Column(nullable = false)
  String organisationId;

  public OrganisationMembershipKeyDao() {}

  public OrganisationMembershipKeyDao(@NotNull String userId, @NotNull String organisationId) {
    this.userId = userId;
    this.organisationId = organisationId;
  }

  public String getUserId() {
    return userId;
  }

  public OrganisationMembershipKeyDao userId(@NotNull String userId) {
    this.userId = userId;
    return this;
  }

  public String getOrganisationId() {
    return organisationId;
  }

  public OrganisationMembershipKeyDao organisationId(@NotNull String organisationId) {
    this.organisationId = organisationId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrganisationMembershipKeyDao that = (OrganisationMembershipKeyDao) o;

    if (!getUserId().equals(that.getUserId())) return false;
    return getOrganisationId().equals(that.getOrganisationId());
  }

  @Override
  public int hashCode() {
    int result = getUserId().hashCode();
    result = 31 * result + getOrganisationId().hashCode();
    return result;
  }
}
