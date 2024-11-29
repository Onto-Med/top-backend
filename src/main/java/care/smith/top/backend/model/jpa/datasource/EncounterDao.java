package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity(name = "encounter")
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(EncounterDao.EncounterKey.class)
public class EncounterDao {
  @Id private String dataSourceId;
  @Id private String encounterId;

  private String subjectId;
  private String type;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;

  public EncounterDao() {}

  public EncounterDao(
      String dataSourceId,
      String encounterId,
      String subjectId,
      String type,
      OffsetDateTime startDateTime,
      OffsetDateTime endDateTime) {
    this.dataSourceId = dataSourceId;
    this.encounterId = encounterId;
    this.subjectId = subjectId;
    this.type = type;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataSourceId, encounterId, endDateTime, startDateTime, subjectId, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EncounterDao other = (EncounterDao) obj;
    return Objects.equals(dataSourceId, other.dataSourceId)
        && Objects.equals(encounterId, other.encounterId)
        && Objects.equals(endDateTime, other.endDateTime)
        && Objects.equals(startDateTime, other.startDateTime)
        && Objects.equals(subjectId, other.subjectId)
        && Objects.equals(type, other.type);
  }

  public EncounterDao dataSourceId(String dataSourceId) {
    this.dataSourceId = dataSourceId;
    return this;
  }

  public EncounterDao encounterId(String encounterId) {
    this.encounterId = encounterId;
    return this;
  }

  public EncounterDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public EncounterDao type(String type) {
    this.type = type;
    return this;
  }

  public EncounterDao startDateTime(OffsetDateTime startDateTime) {
    this.startDateTime = startDateTime;
    return this;
  }

  public EncounterDao endDateTime(OffsetDateTime endDateTime) {
    this.endDateTime = endDateTime;
    return this;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public String getEncounterId() {
    return encounterId;
  }

  public String getSubjectId() {
    return subjectId;
  }

  public String getType() {
    return type;
  }

  public OffsetDateTime getStartDateTime() {
    return startDateTime;
  }

  public OffsetDateTime getEndDateTime() {
    return endDateTime;
  }

  public static class EncounterKey implements Serializable {
    private final String dataSourceId;
    private final String encounterId;

    public EncounterKey(String dataSourceId, String encounterId) {
      this.dataSourceId = dataSourceId;
      this.encounterId = encounterId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(dataSourceId, encounterId);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      EncounterKey other = (EncounterKey) obj;
      return Objects.equals(dataSourceId, other.dataSourceId)
          && Objects.equals(encounterId, other.encounterId);
    }
  }
}
