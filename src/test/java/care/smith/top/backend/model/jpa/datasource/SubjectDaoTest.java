package care.smith.top.backend.model.jpa.datasource;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.AbstractTest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration
class SubjectDaoTest extends AbstractTest {

  @Test
  void testCreateAndDeleteSubject() {
    String dataSourceId = "data_source_1";
    String subjectId = "subject_1";

    SubjectDao subject = new SubjectDao(dataSourceId, subjectId, OffsetDateTime.now(), "female");
    subjectRepository.save(subject);

    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId))
        .isNotEmpty();

    // This should fail, because no subject with ID "subject_2" exists:
    assertThat(subjectRepository.findById(new SubjectDao.SubjectKey(dataSourceId, "subject_2")))
        .isEmpty();

    assertThatCode(() -> subjectRepository.delete(subject)).doesNotThrowAnyException();
    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId)).isEmpty();
  }
}
