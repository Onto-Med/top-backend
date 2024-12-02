package care.smith.top.backend.repository.jpa.datasource;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration
class SubjectRepositoryTest extends AbstractTest {

  @Test
  void testfindByDataSourceIdAndSubjectId() {
    String dataSourceId = "data_source_1";
    String subjectId = "subject_1";

    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId)).isEmpty();

    SubjectDao subject = new SubjectDao(dataSourceId, subjectId, OffsetDateTime.now(), "female");
    subjectRepository.save(subject);

    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId))
        .isNotEmpty();

    // This should fail, because no subject with ID "subject_2" exists:
    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, "subject_2"))
        .isEmpty();

    assertThatCode(() -> subjectRepository.delete(subject)).doesNotThrowAnyException();
    assertThat(subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId)).isEmpty();
  }
}
