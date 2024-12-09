package care.smith.top.backend.service.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.top_document_query.util.DateUtil;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
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
        new SubjectCSVImport(dataSourceId, reader, subjectRepository, fieldsMaping);
    subjectCSVImport.run();

    List<SubjectDao> subjects = subjectRepository.findAll();

    assertEquals(2, subjects.size());
    assertEquals(
        subjects.get(0), new SubjectDao(dataSourceId, "p1", DateUtil.parse("1985-01-02"), "male"));
    assertEquals(
        subjects.get(1),
        new SubjectDao(dataSourceId, "p2", DateUtil.parse("05.06.2001"), "female"));
  }

  @Test
  void testSubjectResourceImport() {
    String csv =
        "res_id; enc_id; pat_id; code; code_system; timestamp; value"
            + System.lineSeparator()
            + "1; e1; p1; code1; codesystem1; 01.02.2003; 12.34"
            + System.lineSeparator()
            + "2; e2; p2; code2; codesystem2; 2004-05-06T01:02:03; 56.78";

    Reader reader = new StringReader(csv);

    Map<String, String> fieldsMaping =
        Map.of(
            "res_id",
            "subjectResourceId",
            "enc_id",
            "encounterId",
            "pat_id",
            "subjectId",
            "code",
            "code",
            "code_system",
            "codeSystem",
            "timestamp",
            "dateTime",
            "value",
            "numberValue");

    SubjectResourceCSVImport subjectResourceCSVImport =
        new SubjectResourceCSVImport(
            dataSourceId,
            reader,
            subjectRepository,
            encounterRepository,
            subjectResourceRepository,
            fieldsMaping);
    subjectResourceCSVImport.run();

    SubjectDao sub1 = new SubjectDao(dataSourceId, "p1");
    SubjectDao sub2 = new SubjectDao(dataSourceId, "p2");

    EncounterDao enc1 = new EncounterDao(dataSourceId, "e1", sub1);
    EncounterDao enc2 = new EncounterDao(dataSourceId, "e2", sub2);

    SubjectResourceDao res1 =
        new SubjectResourceDao(dataSourceId, "1", sub1, enc1, "codesystem1", "code1")
            .dateTime(DateUtil.parse("01.02.2003"))
            .numberValue(new BigDecimal("12.34"));
    SubjectResourceDao res2 =
        new SubjectResourceDao(dataSourceId, "2", sub2, enc2, "codesystem2", "code2")
            .dateTime(DateUtil.parse("2004-05-06T01:02:03"))
            .numberValue(new BigDecimal("56.78"));

    List<SubjectDao> subjects = subjectRepository.findAll();
    assertEquals(2, subjects.size());
    assertEquals(subjects.get(0), sub1);
    assertEquals(subjects.get(1), sub2);

    List<EncounterDao> encounters = encounterRepository.findAll();
    assertEquals(2, encounters.size());
    assertEquals(encounters.get(0), enc1);
    assertEquals(encounters.get(1), enc2);

    List<SubjectResourceDao> resources = subjectResourceRepository.findAll();
    assertEquals(2, resources.size());
    assertEquals(resources.get(0), res1);
    assertEquals(resources.get(1), res2);
  }
}
