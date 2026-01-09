package care.smith.top.backend.model.jpa.datasource;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(name = "subject")
@Table(indexes = @Index(columnList = "dataSourceId"))
public class SubjectDao {
  @EmbeddedId private SubjectKey subjectKey;

  private LocalDateTime birthDate;
  private String sex;

  @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
  private List<EncounterDao> encounters;

  @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
  private List<SubjectResourceDao> subjectResources;

  public SubjectDao() {}

  public SubjectDao(@NotNull String dataSourceId, String subjectId) {
    subjectKey = new SubjectKey(dataSourceId, subjectId);
  }

  public SubjectDao(
      @NotNull String dataSourceId, String subjectId, LocalDateTime birthDate, String sex) {
    this(dataSourceId, subjectId);
    this.birthDate = birthDate;
    this.sex = sex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(birthDate, sex, subjectKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SubjectDao other = (SubjectDao) obj;
    return Objects.equals(birthDate, other.birthDate)
        && Objects.equals(sex, other.sex)
        && Objects.equals(subjectKey, other.subjectKey);
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("[SubjectDao|" + getDataSourceId() + "|" + getSubjectId());
    if (birthDate != null) sb.append("|" + birthDate);
    if (sex != null) sb.append("|" + sex);
    return sb.append("]").toString();
  }

  public SubjectDao dataSourceId(@NotNull String dataSourceId) {
    subjectKey.dataSourceId(dataSourceId);
    return this;
  }

  public SubjectDao subjectId(String subjectId) {
    subjectKey.setSubjectId(subjectId);
    return this;
  }

  public SubjectDao birthDate(LocalDateTime birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  public SubjectDao sex(String sex) {
    this.sex = sex;
    return this;
  }

  public SubjectDao encounters(List<EncounterDao> encounterDaos) {
    this.encounters = encounterDaos;
    return this;
  }

  public SubjectDao addEncounter(EncounterDao... encounterDaos) {
    if (encounters == null) encounters = new ArrayList<>();
    encounters.addAll(List.of(encounterDaos));
    return this;
  }

  public SubjectDao subjectResources(List<SubjectResourceDao> subjectResourceDaos) {
    this.subjectResources = subjectResourceDaos;
    return this;
  }

  public SubjectDao addSubjectResource(SubjectResourceDao... subjectResourceDaos) {
    if (subjectResources == null) subjectResources = new ArrayList<>();
    subjectResources.addAll(List.of(subjectResourceDaos));
    return this;
  }

  public String getDataSourceId() {
    return subjectKey.getDataSourceId();
  }

  public String getSubjectId() {
    return subjectKey.getSubjectId();
  }

  public LocalDateTime getBirthDate() {
    return birthDate;
  }

  public String getSex() {
    return sex;
  }

  public List<EncounterDao> getEncounters() {
    return encounters;
  }

  public List<SubjectResourceDao> getSubjectResources() {
    return subjectResources;
  }

  @Embeddable
  public static class SubjectKey implements Serializable {
    private String dataSourceId;
    private String subjectId;

    public SubjectKey() {}

    public SubjectKey(@NotNull String dataSourceId, String subjectId) {
      this.dataSourceId = dataSourceId;
      this.subjectId = subjectId;
    }

    public String getDataSourceId() {
      return dataSourceId;
    }

    public SubjectKey dataSourceId(@NotNull String dataSourceId) {
      this.dataSourceId = dataSourceId;
      return this;
    }

    public String getSubjectId() {
      return subjectId;
    }

    public SubjectKey setSubjectId(String subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    @Override
    public int hashCode() {
      return Objects.hash(dataSourceId, subjectId);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SubjectKey other = (SubjectKey) obj;
      return Objects.equals(getDataSourceId(), other.getDataSourceId())
          && Objects.equals(getSubjectId(), other.getSubjectId());
    }
  }
}
