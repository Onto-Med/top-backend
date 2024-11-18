package care.smith.top.backend.model.datasource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(Observation.ObservationKey.class)
public class Observation {
  @Id private String dataSourceId;
  @Id private Long observationId;

  @NotNull private String subjectId;
  @NotNull private String codeSystem;
  @NotNull private String code;
  private OffsetDateTime createdAt;
  private BigDecimal numberValue;
  private String unit;
  private String textValue;
  private LocalDateTime dateTimeValue;
  private Boolean booleanValue;

  public Observation() {}

  public Observation(
      String dataSourceId, Long observationId, String subjectId, String codeSystem, String code) {
    this.dataSourceId = dataSourceId;
    this.observationId = observationId;
    this.subjectId = subjectId;
    this.codeSystem = codeSystem;
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Observation observation = (Observation) o;
    return getDataSourceId().equals(observation.getDataSourceId())
        && getObservationId().equals(observation.getObservationId())
        && getSubjectId().equals(observation.getSubjectId())
        && getCodeSystem().equals(observation.getCodeSystem())
        && getCode().equals(observation.getCode())
        && Objects.equals(getCreatedAt(), observation.getCreatedAt())
        && Objects.equals(getNumberValue(), observation.getNumberValue())
        && Objects.equals(getUnit(), observation.getUnit())
        && Objects.equals(getTextValue(), observation.getTextValue())
        && Objects.equals(getDateTimeValue(), observation.getDateTimeValue())
        && Objects.equals(getBooleanValue(), observation.getBooleanValue());
  }

  @Override
  public int hashCode() {
    int result = getDataSourceId().hashCode();
    result = 31 * result + getObservationId().hashCode();
    result = 31 * result + getSubjectId().hashCode();
    result = 31 * result + getCodeSystem().hashCode();
    result = 31 * result + getCode().hashCode();
    result = 31 * result + Objects.hashCode(getCreatedAt());
    result = 31 * result + Objects.hashCode(getNumberValue());
    result = 31 * result + Objects.hashCode(getUnit());
    result = 31 * result + Objects.hashCode(getTextValue());
    result = 31 * result + Objects.hashCode(getDateTimeValue());
    result = 31 * result + Objects.hashCode(getBooleanValue());
    return result;
  }

  public Observation textValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public Observation unit(String unit) {
    this.unit = unit;
    return this;
  }

  public Observation createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Observation numberValue(BigDecimal numberValue) {
    this.numberValue = numberValue;
    return this;
  }

  public Observation booleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
    return this;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public Long getObservationId() {
    return observationId;
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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public BigDecimal getNumberValue() {
    return numberValue;
  }

  public String getCode() {
    return code;
  }

  public static class ObservationKey implements Serializable {
    private final String dataSourceId;
    private final Long observationId;

    public ObservationKey(String dataSourceId, Long observationId) {
      this.dataSourceId = dataSourceId;
      this.observationId = observationId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ObservationKey that = (ObservationKey) o;
      return dataSourceId.equals(that.dataSourceId) && observationId.equals(that.observationId);
    }

    @Override
    public int hashCode() {
      int result = dataSourceId.hashCode();
      result = 31 * result + observationId.hashCode();
      return result;
    }
  }
}
