package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity(name = "encounter")
@Table(indexes = @Index(columnList = "dataSourceId"))
public class EncounterDao {
  @EmbeddedId private EncounterKey encounterKey;

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

  private String type;
  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;

  @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL)
  private List<SubjectResourceDao> subjectResources = null;

  public EncounterDao() {}

  public EncounterDao(@NotNull String dataSourceId, String encounterId) {
    encounterKey = new EncounterKey(dataSourceId, encounterId);
  }

  public EncounterDao(@NotNull String dataSourceId, String encounterId, SubjectDao subject) {
    this(dataSourceId, encounterId);
    if (subject != null) {
      this.subject = subject;
      subjectId = subject.getSubjectId();
    }
  }

  public EncounterDao(
      @NotNull String dataSourceId,
      String encounterId,
      SubjectDao subject,
      String type,
      LocalDateTime startDateTime,
      LocalDateTime endDateTime) {
    this(dataSourceId, encounterId, subject);
    this.type = type;
    this.startDateTime = startDateTime;
    this.endDateTime = endDateTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(encounterKey, endDateTime, startDateTime, subject, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EncounterDao other = (EncounterDao) obj;
    return Objects.equals(getDataSourceId(), other.getDataSourceId())
        && Objects.equals(getEncounterId(), other.getEncounterId())
        && Objects.equals(getEndDateTime(), other.getEndDateTime())
        && Objects.equals(getStartDateTime(), other.getStartDateTime())
        && Objects.equals(getSubject(), other.getSubject())
        && Objects.equals(getType(), other.getType());
  }

  @Override
  public String toString() {
    StringBuffer sb =
        new StringBuffer(
            "[EncounterDao|" + getDataSourceId() + "|" + getEncounterId() + "|" + subject);
    if (type != null) sb.append("|" + type);
    if (startDateTime != null) sb.append("|" + startDateTime);
    if (endDateTime != null) sb.append("|" + endDateTime);
    return sb.append("]").toString();
  }

  public EncounterDao dataSourceId(@NotNull String dataSourceId) {
    encounterKey.dataSourceId(dataSourceId);
    return this;
  }

  public EncounterDao encounterId(String encounterId) {
    encounterKey.encounterId(encounterId);
    return this;
  }

  public EncounterDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public EncounterDao subject(SubjectDao subject) {
    if (subject != null) {
      this.subject = subject;
      subjectId = subject.getSubjectId();
    }
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
    return encounterKey.getDataSourceId();
  }

  public String getEncounterId() {
    return encounterKey.getEncounterId();
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

  @Embeddable
  public static class EncounterKey implements Serializable {
    private String dataSourceId;
    private String encounterId;

    public EncounterKey() {}

    public EncounterKey(@NotNull String dataSourceId, String encounterId) {
      this.dataSourceId = dataSourceId;
      this.encounterId = encounterId;
    }

    public EncounterKey dataSourceId(@NotNull String dataSourceId) {
      this.dataSourceId = dataSourceId;
      return this;
    }

    public EncounterKey encounterId(String encounterId) {
      this.encounterId = encounterId;
      return this;
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
      return Objects.equals(getDataSourceId(), other.getDataSourceId())
          && Objects.equals(getEncounterId(), other.getEncounterId());
    }

    public String getDataSourceId() {
      return dataSourceId;
    }

    public String getEncounterId() {
      return encounterId;
    }
  }
}
