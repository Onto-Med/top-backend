package care.smith.top.backend.model.jpa.datasource;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "subject_resource")
@Table(indexes = @Index(columnList = "dataSourceId"))
public class SubjectResourceDao {
  @EmbeddedId private SubjectResourceKey subjectResourceKey;

  private String subjectId;

  @JoinColumns({
    @JoinColumn(
        name = "dataSourceId",
        referencedColumnName = "dataSourceId",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "subjectId",
        referencedColumnName = "subjectId",
        insertable = false,
        updatable = false)
  })
  @ManyToOne
  private SubjectDao subject;

  private String encounterId;

  @JoinColumns({
    @JoinColumn(
        name = "dataSourceId",
        referencedColumnName = "dataSourceId",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "encounterId",
        referencedColumnName = "encounterId",
        insertable = false,
        updatable = false)
  })
  @ManyToOne
  private EncounterDao encounter;

  @NotNull private String codeSystem;
  @NotNull private String code;

  private LocalDateTime dateTime;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;

  private String unit;
  private BigDecimal numberValue;
  private String textValue;
  private Boolean booleanValue;
  private LocalDateTime dateTimeValue;

  public SubjectResourceDao() {}

  public SubjectResourceDao(@NotNull String dataSourceId, String subjectResourceId) {
    subjectResourceKey = new SubjectResourceKey(dataSourceId, subjectResourceId);
  }

  public SubjectResourceDao(
      @NotNull String dataSourceId,
      @NotNull String subjectResourceId,
      SubjectDao subject,
      @NotNull String codeSystem,
      @NotNull String code) {
    this(dataSourceId, subjectResourceId, subject, null, codeSystem, code);
  }

  public SubjectResourceDao(
      @NotNull String dataSourceId,
      @NotNull String subjectResourceId,
      EncounterDao encounter,
      @NotNull String codeSystem,
      @NotNull String code) {
    this(dataSourceId, subjectResourceId, null, encounter, codeSystem, code);
  }

  public SubjectResourceDao(
      @NotNull String dataSourceId,
      @NotNull String subjectResourceId,
      SubjectDao subject,
      EncounterDao encounter,
      @NotNull String codeSystem,
      @NotNull String code) {
    this(dataSourceId, subjectResourceId);
    if (subject != null) {
      this.subject = subject;
      subjectId = subject.getSubjectId();
    }
    if (encounter != null) {
      this.encounter = encounter;
      encounterId = encounter.getEncounterId();
    }
    this.codeSystem = codeSystem;
    this.code = code;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        booleanValue,
        code,
        codeSystem,
        dateTime,
        dateTimeValue,
        encounter,
        endDateTime,
        numberValue,
        startDateTime,
        subject,
        subjectResourceKey,
        textValue,
        unit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SubjectResourceDao other = (SubjectResourceDao) obj;
    return Objects.equals(getBooleanValue(), other.getBooleanValue())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getCodeSystem(), other.getCodeSystem())
        && Objects.equals(getDataSourceId(), other.getDataSourceId())
        && Objects.equals(getDateTime(), other.getDateTime())
        && Objects.equals(getDateTimeValue(), other.getDateTimeValue())
        && Objects.equals(getEncounter(), other.getEncounter())
        && Objects.equals(getEndDateTime(), other.getEndDateTime())
        && Objects.equals(getNumberValue(), other.getNumberValue())
        && Objects.equals(getStartDateTime(), other.getStartDateTime())
        && Objects.equals(getSubject(), other.getSubject())
        && Objects.equals(getSubjectResourceId(), other.getSubjectResourceId())
        && Objects.equals(getTextValue(), other.getTextValue())
        && Objects.equals(getUnit(), other.getUnit());
  }

  @Override
  public String toString() {
    String ls = System.lineSeparator();
    StringBuffer sb =
        new StringBuffer(
            "SubjectResourceDao|"
                + getDataSourceId()
                + "|"
                + getSubjectResourceId()
                + "|"
                + codeSystem
                + "|"
                + code
                + ls);
    sb.append("--------------------------------------------------" + ls);
    sb.append("subject: " + subject + ls);
    sb.append("encounter: " + encounter + ls);
    if (dateTime != null) sb.append("dateTime: " + dateTime + ls);
    if (startDateTime != null) sb.append("startDateTime: " + startDateTime + ls);
    if (endDateTime != null) sb.append("endDateTime: " + endDateTime + ls);
    if (unit != null) sb.append("unit: " + unit + ls);
    if (numberValue != null) sb.append("numberValue: " + numberValue + ls);
    if (textValue != null) sb.append("textValue: " + textValue + ls);
    if (booleanValue != null) sb.append("booleanValue: " + booleanValue + ls);
    if (dateTimeValue != null) sb.append("dateTimeValue: " + dateTimeValue + ls);
    return sb.toString();
  }

  public SubjectResourceDao dataSourceId(@NotNull String dataSourceId) {
    subjectResourceKey.dataSourceId(dataSourceId);
    return this;
  }

  public SubjectResourceDao subjectResourceId(String subjectResourceId) {
    subjectResourceKey.subjectResourceId(subjectResourceId);
    return this;
  }

  public SubjectResourceDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public SubjectResourceDao subject(SubjectDao subject) {
    if (subject != null) {
      this.subject = subject;
      subjectId = subject.getSubjectId();
    }
    return this;
  }

  public SubjectResourceDao encounterId(String encounterId) {
    this.encounterId = encounterId;
    return this;
  }

  public SubjectResourceDao encounter(EncounterDao encounter) {
    if (encounter != null) {
      this.encounter = encounter;
      encounterId = encounter.getEncounterId();
    }
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
    return dateTime(LocalDateTime.now());
  }

  public SubjectResourceDao dateTime(LocalDateTime dateTime) {
    this.dateTime = dateTime;
    return this;
  }

  public SubjectResourceDao startDateTime(LocalDateTime startDateTime) {
    this.startDateTime = startDateTime;
    return this;
  }

  public SubjectResourceDao endDateTime(LocalDateTime endDateTime) {
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
    return subjectResourceKey.getDataSourceId();
  }

  public String getSubjectResourceId() {
    return subjectResourceKey.getSubjectResourceId();
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

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  public LocalDateTime getEndDateTime() {
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

  @Embeddable
  public static class SubjectResourceKey implements Serializable {
    private String dataSourceId;
    private String subjectResourceId;

    public SubjectResourceKey() {}

    public SubjectResourceKey(@NotNull String dataSourceId, String subjectResourceId) {
      this.dataSourceId = dataSourceId;
      this.subjectResourceId = subjectResourceId;
    }

    public SubjectResourceKey dataSourceId(@NotNull String dataSourceId) {
      this.dataSourceId = dataSourceId;
      return this;
    }

    public SubjectResourceKey subjectResourceId(String subjectResourceId) {
      this.subjectResourceId = subjectResourceId;
      return this;
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
      return Objects.equals(getDataSourceId(), other.getDataSourceId())
          && Objects.equals(getSubjectResourceId(), other.getSubjectResourceId());
    }

    public String getDataSourceId() {
      return dataSourceId;
    }

    public String getSubjectResourceId() {
      return subjectResourceId;
    }
  }
}
