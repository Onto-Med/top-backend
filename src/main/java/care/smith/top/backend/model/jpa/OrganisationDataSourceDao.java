package care.smith.top.backend.model.jpa;

import care.smith.top.backend.model.jpa.key.OrganisationDataSourceKeyDao;
import care.smith.top.model.QueryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity(name = "organisation_data_source")
@EntityListeners(AuditingEntityListener.class)
public class OrganisationDataSourceDao {
  @EmbeddedId private OrganisationDataSourceKeyDao id;

  @ManyToOne(optional = false)
  @MapsId("organisationId")
  private OrganisationDao organisation;

  @Column(nullable = false)
  private QueryType queryType;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @CreatedBy
  private UserDao createdBy;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  public OrganisationDataSourceDao() {}

  public OrganisationDataSourceDao(
      @NotNull OrganisationDao organisation, @NotNull String dataSourceId, QueryType queryType) {
    this.id = new OrganisationDataSourceKeyDao(organisation.getId(), dataSourceId);
    this.organisation = organisation;
    this.queryType = queryType;
  }

  public OrganisationDataSourceDao organisation(@NotNull OrganisationDao organisation) {
    this.organisation = organisation;
    return this;
  }

  public OrganisationDataSourceDao dataSourceId(@NotNull String dataSourceId) {
    this.id.dataSourceId(dataSourceId);
    return this;
  }

  public OrganisationDataSourceDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public OrganisationDataSourceDao author(UserDao createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrganisationDataSourceDao that = (OrganisationDataSourceDao) o;

    if (!getOrganisation().equals(that.getOrganisation())) return false;
    if (!getQueryType().equals(that.getQueryType())) return false;
    return getDataSourceId().equals(that.getDataSourceId());
  }

  @Override
  public int hashCode() {
    int result = getOrganisation().hashCode();
    result = 31 * result + getQueryType().hashCode();
    result = 31 * result + getDataSourceId().hashCode();
    return result;
  }

  public OrganisationDao getOrganisation() {
    return organisation;
  }

  public QueryType getQueryType() {
    return queryType;
  }

  public String getDataSourceId() {
    return id.getDataSourceId();
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public UserDao getCreatedBy() {
    return createdBy;
  }
}
