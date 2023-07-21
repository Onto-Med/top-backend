package care.smith.top.backend.model.jpa;

import care.smith.top.model.DateTimeRestriction;
import care.smith.top.model.ProjectionEntry;
import care.smith.top.model.QueryCriterion;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

@Embeddable
public class QueryCriterionDao {
  @NotNull private String subjectId;
  private Boolean inclusion = true;
  private String defaultAggregationFunctionId;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private RestrictionDao dateTimeRestriction;

  public QueryCriterionDao() {}

  public QueryCriterionDao(
      @NotNull String subjectId,
      boolean inclusion,
      String defaultAggregationFunctionId,
      DateTimeRestriction dateTimeRestriction) {
    this.subjectId = subjectId;
    this.inclusion = inclusion;
    this.defaultAggregationFunctionId = defaultAggregationFunctionId;
    this.dateTimeRestriction = new RestrictionDao(dateTimeRestriction);
  }

  public QueryCriterionDao(@NotNull QueryCriterion queryCriterion) {
    subjectId = queryCriterion.getSubjectId();
    inclusion = queryCriterion.isInclusion();
    defaultAggregationFunctionId = queryCriterion.getDefaultAggregationFunctionId();
    if (queryCriterion.getDateTimeRestriction() != null)
      dateTimeRestriction = new RestrictionDao(queryCriterion.getDateTimeRestriction());
  }

  public String getSubjectId() {
    return subjectId;
  }

  public QueryCriterionDao subjectId(@NotNull String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public Boolean isInclusion() {
    return inclusion;
  }

  public QueryCriterionDao inclusion(Boolean inclusion) {
    this.inclusion = inclusion;
    return this;
  }

  public String getDefaultAggregationFunctionId() {
    return defaultAggregationFunctionId;
  }

  public QueryCriterionDao defaultAggregationFunctionId(String defaultAggregationFunctionId) {
    this.defaultAggregationFunctionId = defaultAggregationFunctionId;
    return this;
  }

  public RestrictionDao getDateTimeRestriction() {
    return dateTimeRestriction;
  }

  public QueryCriterionDao dateTimeRestriction(RestrictionDao dateTimeRestriction) {
    this.dateTimeRestriction = dateTimeRestriction;
    return this;
  }

  public QueryCriterion toApiModel() {
    QueryCriterion queryCriterion =
        (QueryCriterion)
            new QueryCriterion()
                .inclusion(isInclusion())
                .subjectId(getSubjectId())
                .defaultAggregationFunctionId(getDefaultAggregationFunctionId())
                .type(ProjectionEntry.TypeEnum.QUERYCRITERION);
    if (getDateTimeRestriction() != null)
      queryCriterion.dateTimeRestriction(
          (DateTimeRestriction) getDateTimeRestriction().toApiModel());
    return queryCriterion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryCriterionDao that = (QueryCriterionDao) o;
    return getSubjectId().equals(that.getSubjectId())
        && inclusion.equals(that.inclusion)
        && getDefaultAggregationFunctionId().equals(that.getDefaultAggregationFunctionId())
        && Objects.equals(getDateTimeRestriction(), that.getDateTimeRestriction());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getSubjectId(), inclusion, getDefaultAggregationFunctionId(), getDateTimeRestriction());
  }
}
