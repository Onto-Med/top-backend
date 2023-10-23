package care.smith.top.backend.model.jpa;

import care.smith.top.backend.model.jpa.key.OrganisationDataSourceKeyDao;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Entity(name = "organisation_data_source")
@EntityListeners(AuditingEntityListener.class)
public class OrganisationDataSourceDao {
  @EmbeddedId private OrganisationDataSourceKeyDao id;

  @ManyToOne(optional = false)
  @MapsId("organisationId")
  private OrganisationDao organisation;

  private String md5Hash;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @CreatedBy
  private UserDao createdBy;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  public OrganisationDataSourceDao() {}

  public OrganisationDataSourceDao(
      @NotNull OrganisationDao organisation, @NotNull String dataSourceId, String md5Hash) {
    this.id = new OrganisationDataSourceKeyDao(organisation.getId(), dataSourceId);
    this.organisation = organisation;
    this.md5Hash = md5Hash;
  }

  public OrganisationDataSourceDao organisation(@NotNull OrganisationDao organisation) {
    this.organisation = organisation;
    return this;
  }

  public OrganisationDataSourceDao dataSourceId(@NotNull String dataSourceId) {
    this.id.dataSourceId(dataSourceId);
    return this;
  }

  public OrganisationDataSourceDao md5Hash(@NotNull String md5Hash) {
    this.md5Hash = md5Hash;
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

  public OrganisationDao getOrganisation() {
    return organisation;
  }

  public String getDataSourceId() {
    return id.getDataSourceId();
  }

  public String getMd5Hash() {
    return md5Hash;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public UserDao getCreatedBy() {
    return createdBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrganisationDataSourceDao that = (OrganisationDataSourceDao) o;

    if (!getOrganisation().equals(that.getOrganisation())) return false;
    return getDataSourceId().equals(that.getDataSourceId());
  }

  @Override
  public int hashCode() {
    int result = getOrganisation().hashCode();
    result = 31 * result + getDataSourceId().hashCode();
    return result;
  }
}
