package care.smith.top.backend.service.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.top_document_query.util.DateUtil;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration
class CSVImportTest extends AbstractTest {
  String dataSourceId = "data_source_1";

  @Test
  void testSubjectImport() {
    String csv =
        "pat_id; birth_date; gender"
            + System.lineSeparator()
            + "p1; 1985-01-02; male"
            + System.lineSeparator()
            + "p2; 05.06.2001; female";

    Reader reader = new StringReader(csv);

    Map<String, String> fieldsMaping =
        Map.of("pat_id", "subjectId", "birth_date", "birthDate", "gender", "sex");

    SubjectCSVImport subjectCSVImport =
        new SubjectCSVImport(subjectRepository, dataSourceId, fieldsMaping, reader);
    subjectCSVImport.run();

    List<SubjectDao> subjects = subjectRepository.findAll();

    assertEquals(2, subjects.size());
    assertEquals(
        subjects.get(0), new SubjectDao(dataSourceId, "p1", DateUtil.parse("1985-01-02"), "male"));
    assertEquals(
        subjects.get(1),
        new SubjectDao(dataSourceId, "p2", DateUtil.parse("05.06.2001"), "female"));
  }
}
