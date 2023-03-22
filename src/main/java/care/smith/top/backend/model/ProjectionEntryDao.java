package care.smith.top.backend.model;

import care.smith.top.model.DateTimeRestriction;
import care.smith.top.model.ProjectionEntry;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Embeddable
public class ProjectionEntryDao {
  private String subjectId;
  private String defaultAggregationFunctionId;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private RestrictionDao dateTimeRestriction;

  public ProjectionEntryDao() {}

  public ProjectionEntryDao(
      String subjectId,
      String defaultAggregationFunctionId,
      DateTimeRestriction dateTimeRestriction) {
    this.subjectId = subjectId;
    this.defaultAggregationFunctionId = defaultAggregationFunctionId;
    this.dateTimeRestriction = new RestrictionDao(dateTimeRestriction);
  }

  public ProjectionEntryDao(@NotNull ProjectionEntry projectionEntry) {
    subjectId = projectionEntry.getSubjectId();
    if (projectionEntry.getDateTimeRestriction() != null)
      dateTimeRestriction = new RestrictionDao(projectionEntry.getDateTimeRestriction());
  }

  public String getSubjectId() {
    return subjectId;
  }

  public ProjectionEntryDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public RestrictionDao getDateTimeRestriction() {
    return dateTimeRestriction;
  }

  public ProjectionEntryDao dateTimeRestriction(RestrictionDao dateTimeRestriction) {
    this.dateTimeRestriction = dateTimeRestriction;
    return this;
  }

  public String getDefaultAggregationFunctionId() {
    return defaultAggregationFunctionId;
  }

  public ProjectionEntryDao defaultAggregationFunctionId(String defaultAggregationFunctionId) {
    this.defaultAggregationFunctionId = defaultAggregationFunctionId;
    return this;
  }

  public ProjectionEntry toApiModel() {
    ProjectionEntry entry = new ProjectionEntry().subjectId(getSubjectId());
    if (getDateTimeRestriction() != null)
      entry.dateTimeRestriction((DateTimeRestriction) getDateTimeRestriction().toApiModel());
    return entry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionEntryDao that = (ProjectionEntryDao) o;
    return getSubjectId().equals(that.getSubjectId())
        && Objects.equals(getDateTimeRestriction(), that.getDateTimeRestriction());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSubjectId(), getDateTimeRestriction());
  }
}
