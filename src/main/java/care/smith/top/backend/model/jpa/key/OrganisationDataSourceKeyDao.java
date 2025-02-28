package care.smith.top.backend.model.jpa.key;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class OrganisationDataSourceKeyDao implements Serializable {
  @Column(nullable = false)
  String organisationId;

  @Column(nullable = false)
  String dataSourceId;

  public OrganisationDataSourceKeyDao() {}

  public OrganisationDataSourceKeyDao(
      @NotNull String organisationId, @NotNull String dataSourceId) {
    this.organisationId = organisationId;
    this.dataSourceId = dataSourceId;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public OrganisationDataSourceKeyDao dataSourceId(@NotNull String dataSourceId) {
    this.dataSourceId = dataSourceId;
    return this;
  }

  public String getOrganisationId() {
    return organisationId;
  }

  public OrganisationDataSourceKeyDao organisationId(@NotNull String organisationId) {
    this.organisationId = organisationId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrganisationDataSourceKeyDao that = (OrganisationDataSourceKeyDao) o;

    if (!getOrganisationId().equals(that.getOrganisationId())) return false;
    return getDataSourceId().equals(that.getDataSourceId());
  }

  @Override
  public int hashCode() {
    int result = getOrganisationId().hashCode();
    result = 31 * result + getDataSourceId().hashCode();
    return result;
  }
}
