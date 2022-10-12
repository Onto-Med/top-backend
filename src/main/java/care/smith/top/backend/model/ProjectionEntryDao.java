package care.smith.top.backend.model;

import care.smith.top.model.DateTimeRestriction;
import care.smith.top.model.ProjectionEntry;
import care.smith.top.model.Sorting;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class ProjectionEntryDao {
  private String subjectId;
  private Sorting sorting;
  private DateTimeRestriction dateTimeRestriction = null;

  public ProjectionEntryDao() {}

  public ProjectionEntryDao(
      String subjectId, Sorting sorting, DateTimeRestriction dateTimeRestriction) {
    this.subjectId = subjectId;
    this.sorting = sorting;
    this.dateTimeRestriction = dateTimeRestriction;
  }

  public ProjectionEntryDao(ProjectionEntry projectionEntry) {
    subjectId = projectionEntry.getSubjectId();
    sorting = projectionEntry.getSorting();
    dateTimeRestriction = projectionEntry.getDateTimeRestriction();
  }

  public String getSubjectId() {
    return subjectId;
  }

  public ProjectionEntryDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public Sorting getSorting() {
    return sorting;
  }

  public ProjectionEntryDao sorting(Sorting sorting) {
    this.sorting = sorting;
    return this;
  }

  public DateTimeRestriction getDateTimeRestriction() {
    return dateTimeRestriction;
  }

  public ProjectionEntryDao dateTimeRestriction(DateTimeRestriction dateTimeRestriction) {
    this.dateTimeRestriction = dateTimeRestriction;
    return this;
  }

  public ProjectionEntry toApiModel() {
    return new ProjectionEntry()
        .subjectId(getSubjectId())
        .sorting(getSorting())
        .dateTimeRestriction(getDateTimeRestriction());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionEntryDao that = (ProjectionEntryDao) o;
    return getSubjectId().equals(that.getSubjectId())
        && getSorting() == that.getSorting()
        && Objects.equals(getDateTimeRestriction(), that.getDateTimeRestriction());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSubjectId(), getSorting(), getDateTimeRestriction());
  }
}
