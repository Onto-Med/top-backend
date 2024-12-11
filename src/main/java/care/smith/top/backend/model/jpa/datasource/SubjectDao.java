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
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity(name = "subject")
@Table(indexes = @Index(columnList = "dataSourceId"))
@IdClass(SubjectDao.SubjectKey.class)
public class SubjectDao {
  @Id private String dataSourceId;
  @Id private String subjectId;

  private LocalDateTime birthDate;
  private String sex;

  @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
  private List<EncounterDao> encounters;

  @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
  private List<SubjectResourceDao> subjectResources;

  public SubjectDao() {}

  public SubjectDao(String dataSourceId) {
    this.dataSourceId = dataSourceId;
  }

  public SubjectDao(String dataSourceId, String subjectId) {
    this.dataSourceId = dataSourceId;
    this.subjectId = subjectId;
  }

  public SubjectDao(String dataSourceId, String subjectId, LocalDateTime birthDate, String sex) {
    this.dataSourceId = dataSourceId;
    this.subjectId = subjectId;
    this.birthDate = birthDate;
    this.sex = sex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(birthDate, dataSourceId, sex, subjectId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SubjectDao other = (SubjectDao) obj;
    return Objects.equals(birthDate, other.birthDate)
        && Objects.equals(dataSourceId, other.dataSourceId)
        && Objects.equals(sex, other.sex)
        && Objects.equals(subjectId, other.subjectId);
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("[SubjectDao|" + dataSourceId + "|" + subjectId);
    if (birthDate != null) sb.append("|" + birthDate);
    if (sex != null) sb.append("|" + sex);
    return sb.append("]").toString();
  }

  public SubjectDao dataSourceId(String dataSourceId) {
    this.dataSourceId = dataSourceId;
    return this;
  }

  public SubjectDao subjectId(String subjectId) {
    this.subjectId = subjectId;
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
    return dataSourceId;
  }

  public String getSubjectId() {
    return subjectId;
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

  public static class SubjectKey implements Serializable {
    private String dataSourceId;
    private String subjectId;

    public SubjectKey() {}

    public SubjectKey(String dataSourceId, String subjectId) {
      this.dataSourceId = dataSourceId;
      this.subjectId = subjectId;
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
      return Objects.equals(dataSourceId, other.dataSourceId)
          && Objects.equals(subjectId, other.subjectId);
    }
  }
}
