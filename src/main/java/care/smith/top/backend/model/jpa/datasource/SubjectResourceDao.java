package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity(name = "subject_resource")
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(SubjectResourceDao.SubjectResourceKey.class)
public class SubjectResourceDao {
  @Id private String dataSourceId;
  @Id private Long subjectResourceId;

  @NotNull private String subjectId;
  @NotNull private String codeSystem;
  @NotNull private String code;
  private OffsetDateTime dateTime;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private BigDecimal numberValue;
  private String unit;
  private String textValue;
  private LocalDateTime dateTimeValue;
  private Boolean booleanValue;

  public SubjectResourceDao() {}

  public SubjectResourceDao(
      String dataSourceId,
      Long subjectResourceId,
      String subjectId,
      String codeSystem,
      String code) {
    this.dataSourceId = dataSourceId;
    this.subjectResourceId = subjectResourceId;
    this.subjectId = subjectId;
    this.codeSystem = codeSystem;
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SubjectResourceDao that = (SubjectResourceDao) o;
    return getDataSourceId().equals(that.getDataSourceId())
        && getSubjectResourceId().equals(that.getSubjectResourceId())
        && getSubjectId().equals(that.getSubjectId())
        && getCodeSystem().equals(that.getCodeSystem())
        && getCode().equals(that.getCode())
        && Objects.equals(getDateTime(), that.getDateTime())
        && Objects.equals(getStartDateTime(), that.getStartDateTime())
        && Objects.equals(getEndDateTime(), that.getEndDateTime())
        && Objects.equals(getNumberValue(), that.getNumberValue())
        && Objects.equals(getUnit(), that.getUnit())
        && Objects.equals(getTextValue(), that.getTextValue())
        && Objects.equals(getDateTimeValue(), that.getDateTimeValue())
        && Objects.equals(getBooleanValue(), that.getBooleanValue());
  }

  @Override
  public int hashCode() {
    int result = getDataSourceId().hashCode();
    result = 31 * result + getSubjectResourceId().hashCode();
    result = 31 * result + getSubjectId().hashCode();
    result = 31 * result + getCodeSystem().hashCode();
    result = 31 * result + getCode().hashCode();
    result = 31 * result + Objects.hashCode(getDateTime());
    result = 31 * result + Objects.hashCode(getStartDateTime());
    result = 31 * result + Objects.hashCode(getEndDateTime());
    result = 31 * result + Objects.hashCode(getNumberValue());
    result = 31 * result + Objects.hashCode(getUnit());
    result = 31 * result + Objects.hashCode(getTextValue());
    result = 31 * result + Objects.hashCode(getDateTimeValue());
    result = 31 * result + Objects.hashCode(getBooleanValue());
    return result;
  }

  public SubjectResourceDao textValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public SubjectResourceDao unit(String unit) {
    this.unit = unit;
    return this;
  }

  public SubjectResourceDao dateTime(OffsetDateTime dateTime) {
    this.dateTime = dateTime;
    return this;
  }

  public SubjectResourceDao startDateTime(OffsetDateTime startDateTime) {
    this.startDateTime = startDateTime;
    return this;
  }

  public SubjectResourceDao endDateTime(OffsetDateTime endDateTime) {
    this.endDateTime = endDateTime;
    return this;
  }

  public SubjectResourceDao numberValue(BigDecimal numberValue) {
    this.numberValue = numberValue;
    return this;
  }

  public SubjectResourceDao booleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
    return this;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public Long getSubjectResourceId() {
    return subjectResourceId;
  }

  public String getSubjectId() {
    return subjectId;
  }

  public String getCodeSystem() {
    return codeSystem;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public LocalDateTime getDateTimeValue() {
    return dateTimeValue;
  }

  public String getTextValue() {
    return textValue;
  }

  public String getUnit() {
    return unit;
  }

  public OffsetDateTime getDateTime() {
    return dateTime;
  }

  public OffsetDateTime getStartDateTime() {
    return startDateTime;
  }

  public OffsetDateTime getEndDateTime() {
    return endDateTime;
  }

  public BigDecimal getNumberValue() {
    return numberValue;
  }

  public String getCode() {
    return code;
  }

  public static class SubjectResourceKey implements Serializable {
    private final String dataSourceId;
    private final Long subjectResourceId;

    public SubjectResourceKey(String dataSourceId, Long subjectResourceId) {
      this.dataSourceId = dataSourceId;
      this.subjectResourceId = subjectResourceId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SubjectResourceKey that = (SubjectResourceKey) o;
      return dataSourceId.equals(that.dataSourceId)
          && subjectResourceId.equals(that.subjectResourceId);
    }

    @Override
    public int hashCode() {
      int result = dataSourceId.hashCode();
      result = 31 * result + subjectResourceId.hashCode();
      return result;
    }
  }
}
