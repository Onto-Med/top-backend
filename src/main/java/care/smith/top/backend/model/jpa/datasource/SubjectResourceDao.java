package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Entity(name = "subject_resource")
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(SubjectResourceDao.SubjectResourceKey.class)
public class SubjectResourceDao {
  @Id private String dataSourceId;
  @Id private String subjectResourceId;

  @Transient private String subjectId;
  @ManyToOne private SubjectDao subject;

  @Transient private String encounterId;
  @ManyToOne private EncounterDao encounter;

  @NotNull private String codeSystem;
  @NotNull private String code;

  private OffsetDateTime dateTime;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;

  private String unit;
  private BigDecimal numberValue;
  private String textValue;
  private Boolean booleanValue;
  private LocalDateTime dateTimeValue;

  public SubjectResourceDao() {}

  public SubjectResourceDao(
      @NotNull String dataSourceId,
      SubjectDao subject,
      @NotNull String codeSystem,
      @NotNull String code) {
    this.dataSourceId = dataSourceId;
    this.subject = subject;
    this.codeSystem = codeSystem;
    this.code = code;
  }

  public SubjectResourceDao(
      @NotNull String dataSourceId,
      EncounterDao encounter,
      @NotNull String codeSystem,
      @NotNull String code) {
    this.dataSourceId = dataSourceId;
    this.encounter = encounter;
    this.codeSystem = codeSystem;
    this.code = code;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        booleanValue,
        code,
        codeSystem,
        dataSourceId,
        dateTime,
        dateTimeValue,
        encounter,
        endDateTime,
        numberValue,
        startDateTime,
        subject,
        subjectResourceId,
        textValue,
        unit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SubjectResourceDao other = (SubjectResourceDao) obj;
    return Objects.equals(booleanValue, other.booleanValue)
        && Objects.equals(code, other.code)
        && Objects.equals(codeSystem, other.codeSystem)
        && Objects.equals(dataSourceId, other.dataSourceId)
        && Objects.equals(dateTime, other.dateTime)
        && Objects.equals(dateTimeValue, other.dateTimeValue)
        && Objects.equals(encounter, other.encounter)
        && Objects.equals(endDateTime, other.endDateTime)
        && Objects.equals(numberValue, other.numberValue)
        && Objects.equals(startDateTime, other.startDateTime)
        && Objects.equals(subject, other.subject)
        && Objects.equals(subjectResourceId, other.subjectResourceId)
        && Objects.equals(textValue, other.textValue)
        && Objects.equals(unit, other.unit);
  }

  public SubjectResourceDao dataSourceId(String dataSourceId) {
    this.dataSourceId = dataSourceId;
    return this;
  }

  public SubjectResourceDao subjectResourceId(String subjectResourceId) {
    this.subjectResourceId = subjectResourceId;
    return this;
  }

  public SubjectResourceDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public SubjectResourceDao subject(SubjectDao subject) {
    this.subject = subject;
    return this;
  }

  public SubjectResourceDao encounterId(String encounterId) {
    this.encounterId = encounterId;
    return this;
  }

  public SubjectResourceDao encounter(EncounterDao encounter) {
    this.encounter = encounter;
    return this;
  }

  public SubjectResourceDao codeSystem(String codeSystem) {
    this.codeSystem = codeSystem;
    return this;
  }

  public SubjectResourceDao code(String code) {
    this.code = code;
    return this;
  }

  public SubjectResourceDao now() {
    return dateTime(OffsetDateTime.now());
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

  public SubjectResourceDao unit(String unit) {
    this.unit = unit;
    return this;
  }

  public SubjectResourceDao numberValue(BigDecimal numberValue) {
    this.numberValue = numberValue;
    return this;
  }

  public SubjectResourceDao textValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public SubjectResourceDao booleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
    return this;
  }

  public SubjectResourceDao dateTimeValue(LocalDateTime dateTimeValue) {
    this.dateTimeValue = dateTimeValue;
    return this;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public String getSubjectResourceId() {
    return subjectResourceId;
  }

  public String getSubjectId() {
    return subjectId;
  }

  public SubjectDao getSubject() {
    return subject;
  }

  public String getEncounterId() {
    return encounterId;
  }

  public EncounterDao getEncounter() {
    return encounter;
  }

  public String getCodeSystem() {
    return codeSystem;
  }

  public String getCode() {
    return code;
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

  public String getUnit() {
    return unit;
  }

  public BigDecimal getNumberValue() {
    return numberValue;
  }

  public String getTextValue() {
    return textValue;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public LocalDateTime getDateTimeValue() {
    return dateTimeValue;
  }

  public static class SubjectResourceKey implements Serializable {
    private String dataSourceId;
    private String subjectResourceId;

    public SubjectResourceKey() {}

    public SubjectResourceKey(String dataSourceId, String subjectResourceId) {
      this.dataSourceId = dataSourceId;
      this.subjectResourceId = subjectResourceId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(dataSourceId, subjectResourceId);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SubjectResourceKey other = (SubjectResourceKey) obj;
      return Objects.equals(dataSourceId, other.dataSourceId)
          && Objects.equals(subjectResourceId, other.subjectResourceId);
    }
  }
}
