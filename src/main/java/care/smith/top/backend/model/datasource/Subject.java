package care.smith.top.backend.model.datasource;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.*;

@Entity
@Table(schema = "data_source", indexes = @Index(columnList = "dataSourceId"))
@IdClass(Subject.SubjectKey.class)
public class Subject {
  @Id private String dataSourceId;
  @Id private String subjectId;

  private OffsetDateTime birthDate;
  private String sex;

  public Subject() {}

  public Subject(String dataSourceId, String subjectId, OffsetDateTime birthDate, String sex) {
    this.dataSourceId = dataSourceId;
    this.subjectId = subjectId;
    this.birthDate = birthDate;
    this.sex = sex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Subject subject = (Subject) o;
    return dataSourceId.equals(subject.dataSourceId)
        && subjectId.equals(subject.subjectId)
        && Objects.equals(birthDate, subject.birthDate)
        && Objects.equals(sex, subject.sex);
  }

  @Override
  public int hashCode() {
    int result = dataSourceId.hashCode();
    result = 31 * result + subjectId.hashCode();
    result = 31 * result + Objects.hashCode(birthDate);
    result = 31 * result + Objects.hashCode(sex);
    return result;
  }

  public Subject birthDate(OffsetDateTime birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  public Subject sex(String sex) {
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
