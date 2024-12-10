package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity(name = "encounter")
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(EncounterDao.EncounterKey.class)
public class EncounterDao {
  @Id private String dataSourceId;
  @Id private String encounterId;

  @Transient private String subjectId;
  @ManyToOne private SubjectDao subject;

  private String type;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;

  @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL)
  private List<SubjectResourceDao> subjectResources = null;

  public EncounterDao() {}

  public EncounterDao(String dataSourceId) {
    this.dataSourceId = dataSourceId;
  }

  public EncounterDao(String dataSourceId, String encounterId) {
    this.dataSourceId = dataSourceId;
    this.encounterId = encounterId;
  }

  public EncounterDao(String dataSourceId, String encounterId, SubjectDao subject) {
    this.dataSourceId = dataSourceId;
    this.encounterId = encounterId;
    this.subject = subject;
  }

  public EncounterDao(
      String dataSourceId,
      String encounterId,
      SubjectDao subject,
      String type,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime) {
    this.dataSourceId = dataSourceId;
    this.encounterId = encounterId;
    this.subject = subject;
    this.type = type;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataSourceId, encounterId, endDateTime, startDateTime, subject, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EncounterDao other = (EncounterDao) obj;
    return Objects.equals(dataSourceId, other.getDataSourceId())
        && Objects.equals(encounterId, other.getEncounterId())
        && Objects.equals(endDateTime, other.getEndDateTime())
        && Objects.equals(startDateTime, other.getStartDateTime())
        && Objects.equals(subject, other.getSubject())
        && Objects.equals(type, other.getType());
  }

  @Override
  public String toString() {
    StringBuffer sb =
        new StringBuffer("[EncounterDao|" + dataSourceId + "|" + encounterId + "|" + subject);
    if (type != null) sb.append("|" + type);
    if (startDateTime != null) sb.append("|" + startDateTime);
    if (endDateTime != null) sb.append("|" + endDateTime);
    return sb.append("]").toString();
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

  public EncounterDao subject(SubjectDao subject) {
    this.subject = subject;
    return this;
  }

  public EncounterDao type(String type) {
    this.type = type;
    return this;
  }

  public EncounterDao startDateTime(LocalDateTime startDateTime) {
    this.startDateTime = startDateTime;
    return this;
  }

  public EncounterDao endDateTime(LocalDateTime endDateTime) {
    this.endDateTime = endDateTime;
    return this;
  }

  public EncounterDao subjectResources(List<SubjectResourceDao> subjectResourceDaos) {
    this.subjectResources = subjectResourceDaos;
    return this;
  }

  public EncounterDao addSubjectRecouse(SubjectResourceDao... subjectResourceDaos) {
    if (subjectResources == null) subjectResources = new ArrayList<>();
    subjectResources.addAll(List.of(subjectResourceDaos));
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

  public SubjectDao getSubject() {
    return subject;
  }

  public String getType() {
    return type;
  }

  public LocalDateTime getStartDateTime() {
    return startDateTime;
  }

  public LocalDateTime getEndDateTime() {
    return endDateTime;
  }

  public List<SubjectResourceDao> getSubjectResources() {
    return subjectResources;
  }

  public static class EncounterKey implements Serializable {
    private String dataSourceId;
    private String encounterId;

    public EncounterKey() {}

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
