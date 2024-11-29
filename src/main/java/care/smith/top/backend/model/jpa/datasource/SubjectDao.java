package care.smith.top.backend.model.jpa.datasource;

import java.io.Serializable;
import java.time.OffsetDateTime;
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

  public SubjectDao birthDate(OffsetDateTime birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  public SubjectDao sex(String sex) {
    this.sex = sex;
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

  public static class SubjectKey implements Serializable {
    private final String dataSourceId;
    private final String subjectId;

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
