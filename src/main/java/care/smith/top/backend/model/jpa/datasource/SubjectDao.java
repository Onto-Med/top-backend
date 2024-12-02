package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;

@Entity(name = "subject")
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(SubjectDao.SubjectKey.class)
public class SubjectDao {
  @Id private String dataSourceId;
  @Id private String subjectId;

  private OffsetDateTime birthDate;
  private String sex;

  @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
  private List<EncounterDao> encounters;

  @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
  private List<SubjectResourceDao> subjectResources;

  public SubjectDao() {}

  public SubjectDao(String dataSourceId, String subjectId, OffsetDateTime birthDate, String sex) {
    this.dataSourceId = dataSourceId;
    this.subjectId = subjectId;
    this.birthDate = birthDate;
    this.sex = sex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SubjectDao subjectDao = (SubjectDao) o;
    return dataSourceId.equals(subjectDao.dataSourceId)
        && subjectId.equals(subjectDao.subjectId)
        && Objects.equals(birthDate, subjectDao.birthDate)
        && Objects.equals(sex, subjectDao.sex);
  }

  @Override
  public int hashCode() {
    int result = dataSourceId.hashCode();
    result = 31 * result + subjectId.hashCode();
    result = 31 * result + Objects.hashCode(birthDate);
    result = 31 * result + Objects.hashCode(sex);
    return result;
  }

  public SubjectDao dataSourceId(String dataSourceId) {
    this.dataSourceId = dataSourceId;
    return this;
  }

  public SubjectDao subjectId(String subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public SubjectDao birthDate(OffsetDateTime birthDate) {
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

  public OffsetDateTime getBirthDate() {
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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SubjectKey that = (SubjectKey) o;
      return dataSourceId.equals(that.dataSourceId) && subjectId.equals(that.subjectId);
    }

    @Override
    public int hashCode() {
      int result = dataSourceId.hashCode();
      result = 31 * result + subjectId.hashCode();
      return result;
    }
  }
}
