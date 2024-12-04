package care.smith.top.backend.repository.jpa.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration
class SubjectRepositoryTest extends AbstractTest {
  String dataSourceId = "data_source_1";

  @Test
  void testfindByDataSourceIdAndSubjectId() {
    String subjectId = "subject_1";

    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId)).isEmpty();

    SubjectDao subject = new SubjectDao(dataSourceId, subjectId, LocalDateTime.now(), "female");
    subjectRepository.save(subject);

    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId))
        .isNotEmpty();

    // This should fail, because no subject with ID "subject_2" exists:
    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, "subject_2"))
        .isEmpty();

    assertThatCode(() -> subjectRepository.delete(subject)).doesNotThrowAnyException();
    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId)).isEmpty();
  }

  @Test
  void testCascadeInsert() {
    String subjectId = "subject_1";

    SubjectDao subject = new SubjectDao(dataSourceId, subjectId, LocalDateTime.now(), "male");

    SubjectResourceDao height =
        new SubjectResourceDao(dataSourceId, subject, "http://loinc.org/", "3137-7").now();

    subject.addSubjectResource(height);

    assertThatCode(() -> subjectRepository.save(subject)).doesNotThrowAnyException();
  }
}
