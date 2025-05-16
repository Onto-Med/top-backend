package care.smith.top.backend.model.jpa.datasource;

import care.smith.top.model.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity(name = "expected_result")
@Table(indexes = @Index(columnList = "dataSourceId"))
public class ExpectedResultDao {
  @EmbeddedId private ExpectedResultKey expectedResultKey;

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

  @NotNull private String phenotypeId;

  private BigDecimal numberValue;
  private String textValue;
  private Boolean booleanValue;
  private LocalDateTime dateTimeValue;

  public ExpectedResultDao() {}

  public ExpectedResultDao(@NotNull String dataSourceId, @NotNull String expectedResultId) {
    expectedResultKey = new ExpectedResultKey(dataSourceId, expectedResultId);
  }

  public ExpectedResultDao(
      @NotNull String dataSourceId,
      @NotNull String expectedResultId,
      SubjectDao subject,
      @NotNull String phenotypeId) {
    this(dataSourceId, expectedResultId, subject, null, phenotypeId);
  }

  public ExpectedResultDao(
      @NotNull String dataSourceId,
      @NotNull String expectedResultId,
      EncounterDao encounter,
      @NotNull String phenotypeId) {
    this(dataSourceId, expectedResultId, null, encounter, phenotypeId);
  }

  public ExpectedResultDao(
      @NotNull String dataSourceId,
      @NotNull String expectedResultId,
      SubjectDao subject,
      EncounterDao encounter,
      @NotNull String phenotypeId) {
    this(dataSourceId, expectedResultId);
    this.subject = subject;
    this.encounter = encounter;
    this.phenotypeId = phenotypeId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        booleanValue,
        dateTimeValue,
        encounter,
        numberValue,
        phenotypeId,
        subject,
        expectedResultKey,
        textValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExpectedResultDao that = (ExpectedResultDao) o;
    return Objects.equals(getExpectedResultId(), that.getExpectedResultId())
        && Objects.equals(getSubjectId(), that.getSubjectId())
        && Objects.equals(getSubject(), that.getSubject())
        && Objects.equals(getEncounterId(), that.getEncounterId())
        && Objects.equals(getEncounter(), that.getEncounter())
        && Objects.equals(getPhenotypeId(), that.getPhenotypeId())
        && Objects.equals(getNumberValue(), that.getNumberValue())
        && Objects.equals(getTextValue(), that.getTextValue())
        && Objects.equals(getBooleanValue(), that.getBooleanValue())
        && Objects.equals(getDateTimeValue(), that.getDateTimeValue());
  }

  @Override
  public String toString() {
    String ls = System.lineSeparator();
    StringBuilder sb =
        new StringBuilder(
            "ExpectedResultDao|"
                + getDataSourceId()
                + "|"
                + getExpectedResultId()
                + "|"
                + phenotypeId
                + ls);
    sb.append("--------------------------------------------------").append(ls);
    sb.append("subject: ").append(subject).append(ls);
    sb.append("encounter: ").append(encounter).append(ls);
    if (numberValue != null) sb.append("numberValue: ").append(numberValue).append(ls);
    if (textValue != null) sb.append("textValue: ").append(textValue).append(ls);
    if (booleanValue != null) sb.append("booleanValue: ").append(booleanValue).append(ls);
    if (dateTimeValue != null) sb.append("dateTimeValue: ").append(dateTimeValue).append(ls);
    return sb.toString();
  }

  public ExpectedResultDao dataSourceId(String dataSourceId) {
    expectedResultKey.dataSourceId(dataSourceId);
    return this;
  }

  public ExpectedResultDao expectedResultId(String expectedResultId) {
    expectedResultKey.expectedResultId(expectedResultId);
    return this;
  }

  public ExpectedResultDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public ExpectedResultDao subject(SubjectDao subject) {
    if (subject != null) {
      this.subject = subject;
      subjectId = subject.getSubjectId();
    }
    return this;
  }

  public ExpectedResultDao encounterId(String encounterId) {
    this.encounterId = encounterId;
    return this;
  }

  public ExpectedResultDao encounter(EncounterDao encounter) {
    if (encounter != null) {
      this.encounter = encounter;
      encounterId = encounter.getEncounterId();
    }
    return this;
  }

  public ExpectedResultDao phenotypeId(String phenotypeId) {
    this.phenotypeId = phenotypeId;
    return this;
  }

  public ExpectedResultDao numberValue(BigDecimal numberValue) {
    this.numberValue = numberValue;
    return this;
  }

  public ExpectedResultDao textValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public ExpectedResultDao booleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
    return this;
  }

  public ExpectedResultDao dateTimeValue(LocalDateTime dateTimeValue) {
    this.dateTimeValue = dateTimeValue;
    return this;
  }

  public String getDataSourceId() {
    return expectedResultKey.getDataSourceId();
  }

  public String getExpectedResultId() {
    return expectedResultKey.getExpectedResultId();
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

  public String getPhenotypeId() {
    return phenotypeId;
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

  public Value toValue() {
    if (getBooleanValue() != null) {
      return new BooleanValue(DataType.BOOLEAN).value(getBooleanValue());
    } else if (getDateTimeValue() != null) {
      return new DateTimeValue(DataType.DATE_TIME).value(getDateTimeValue());
    } else if (getNumberValue() != null) {
      return new NumberValue(DataType.NUMBER).value(getNumberValue());
    } else if (getTextValue() != null) {
      return new StringValue(DataType.STRING).value(getTextValue());
    }
    return new Value();
  }

  public TestReport toReport(Value actual) {
    return new TestReport(
            getExpectedResultId(),
            getPhenotypeId(),
            getSubjectId(),
            getEncounterId(),
            compareWith(actual))
        .expected(toValue())
        .actual(actual);
  }

  private boolean compareWith(Value other) {
    Value value = toValue();
    if (value == other) {
      return true;
    } else if (other == null) {
      return false;
    } else if (Objects.equals(value.getDataType(), other.getDataType())) {
      if (value instanceof BooleanValue) {
        return Objects.equals(((BooleanValue) value).isValue(), ((BooleanValue) other).isValue());
      } else if (value instanceof NumberValue) {
        return Objects.equals(((NumberValue) value).getValue(), ((NumberValue) other).getValue());
      } else if (value instanceof StringValue) {
        return Objects.equals(((StringValue) value).getValue(), ((StringValue) other).getValue());
      } else if (value instanceof DateTimeValue) {
        return Objects.equals(
            ((DateTimeValue) value).getValue(), ((DateTimeValue) other).getValue());
      }
    }
    return false;
  }

  @Embeddable
  public static class ExpectedResultKey implements Serializable {
    private String dataSourceId;
    private String expectedResultId;

    public ExpectedResultKey() {}

    public ExpectedResultKey(@NotNull String dataSourceId, @NotNull String expectedResultId) {
      this.dataSourceId = dataSourceId;
      this.expectedResultId = expectedResultId;
    }

    public ExpectedResultKey dataSourceId(@NotNull String dataSourceId) {
      this.dataSourceId = dataSourceId;
      return this;
    }

    public ExpectedResultKey expectedResultId(@NotNull String expectedResultId) {
      this.expectedResultId = expectedResultId;
      return this;
    }

    @Override
    public int hashCode() {
      return Objects.hash(dataSourceId, expectedResultId);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ExpectedResultKey other = (ExpectedResultKey) obj;
      return Objects.equals(getDataSourceId(), other.getDataSourceId())
          && Objects.equals(getExpectedResultId(), other.getExpectedResultId());
    }

    public String getDataSourceId() {
      return dataSourceId;
    }

    public String getExpectedResultId() {
      return expectedResultId;
    }
  }
}
