package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.top_document_query.util.DateUtil;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CSVImportTest extends ImportTest {
  public static final String SUBJECT_CSV =
      "pat_id; birth_date; gender"
          + System.lineSeparator()
          + "p1; 1985-01-02; male"
          + System.lineSeparator()
          + "p2; 05.06.2001; female";
  public static final String SUBJECT_RESOURCE_CSV =
      "res_id; enc_id; pat_id; code; code_system; timestamp; value"
          + System.lineSeparator()
          + "r1; e1; p1; code1; codesystem1; 01.02.2003; 12.34"
          + System.lineSeparator()
          + "r2; e2; p2; code2; codesystem2; 2004-05-06T01:02:03; 56.78";
  static final Map<String, String> SUBJECT_FIELDS_MAP =
      Map.of("pat_id", "subjectId", "birth_date", "birthDate", "gender", "sex");
  static final Map<String, String> SUBJECT_RESOURCE_FIELDS_MAP =
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
  static final Map<String, String> ENCOUNTER_FIELDS_MAP =
      Map.of(
          "pat_id",
          "subjectId",
          "enc_id",
          "encounterId",
          "type",
          "type",
          "start",
          "startDateTime",
          "end",
          "endDateTime");
  static final String DATA_SOURCE_ID = "data_source_1";
  public static final SubjectDao EXPECTED_SUBJECT_2 =
      new SubjectDao(DATA_SOURCE_ID, "p2", DateUtil.parse("05.06.2001"), "female");
  public static final SubjectDao EXPECTED_SUBJECT_1 =
      new SubjectDao(DATA_SOURCE_ID, "p1", DateUtil.parse("1985-01-02"), "male");

  @Test
  public void testSubjectResourceImport() {
    importSubjectResourceData(SUBJECT_RESOURCE_CSV);

    SubjectDao sub1 = new SubjectDao(DATA_SOURCE_ID, "p1");
    SubjectDao sub2 = new SubjectDao(DATA_SOURCE_ID, "p2");

    EncounterDao enc1 = new EncounterDao(DATA_SOURCE_ID, "e1", sub1);
    EncounterDao enc2 = new EncounterDao(DATA_SOURCE_ID, "e2", sub2);

    SubjectResourceDao res1 = buildExpectedSubjectResource1(sub1, enc1);
    SubjectResourceDao res2 = buildExpectedSubjectResource2(sub2, enc2);

    assertSubjects(sub1, sub2);
    assertEncounters(enc1, enc2);
    assertSubjectResources(res1, res2);
  }

  @Test
  public void testSubjectResourceWithoutEncounterImport() {
    String csv =
        "res_id; pat_id; code; code_system; timestamp; value"
            + System.lineSeparator()
            + "r1; p1; code1; codesystem1; 01.02.2003; 12.34"
            + System.lineSeparator()
            + "r2; p2; code2; codesystem2; 2004-05-06T01:02:03; 56.78";
    importSubjectResourceData(csv);

    SubjectDao sub1 = new SubjectDao(DATA_SOURCE_ID, "p1");
    SubjectDao sub2 = new SubjectDao(DATA_SOURCE_ID, "p2");

    SubjectResourceDao res1 = buildExpectedSubjectResource1(sub1, null);
    SubjectResourceDao res2 = buildExpectedSubjectResource2(sub2, null);

    assertSubjects(sub1, sub2);
    assertEncounters();
    assertSubjectResources(res1, res2);
  }

  @Test
  public void testSubjectResourceAndSubjectImport() {
    importSubjectResourceData(SUBJECT_RESOURCE_CSV);
    importSubjectData(SUBJECT_CSV);

    EncounterDao enc1 = new EncounterDao(DATA_SOURCE_ID, "e1", EXPECTED_SUBJECT_1);
    EncounterDao enc2 = new EncounterDao(DATA_SOURCE_ID, "e2", EXPECTED_SUBJECT_2);

    SubjectResourceDao res1 = buildExpectedSubjectResource1(EXPECTED_SUBJECT_1, enc1);
    SubjectResourceDao res2 = buildExpectedSubjectResource2(EXPECTED_SUBJECT_2, enc2);

    assertSubjects(EXPECTED_SUBJECT_1, EXPECTED_SUBJECT_2);
    assertEncounters(enc1, enc2);
    assertSubjectResources(res1, res2);
  }

  @Test
  public void testSubjectAndSubjectResourceImport() {
    importSubjectData(SUBJECT_CSV);
    importSubjectResourceData(SUBJECT_RESOURCE_CSV);

    EncounterDao enc1 = new EncounterDao(DATA_SOURCE_ID, "e1", EXPECTED_SUBJECT_1);
    EncounterDao enc2 = new EncounterDao(DATA_SOURCE_ID, "e2", EXPECTED_SUBJECT_2);

    SubjectResourceDao res1 = buildExpectedSubjectResource1(EXPECTED_SUBJECT_1, enc1);
    SubjectResourceDao res2 = buildExpectedSubjectResource2(EXPECTED_SUBJECT_2, enc2);

    assertSubjects(EXPECTED_SUBJECT_1, EXPECTED_SUBJECT_2);
    assertEncounters(enc1, enc2);
    assertSubjectResources(res1, res2);
  }

  @Test
  public void testSubjectSubjectResourceAndEncounterImport() {
    importSubjectData(SUBJECT_CSV);
    importSubjectResourceData(SUBJECT_RESOURCE_CSV);
    String csv =
        "enc_id; pat_id; start; end; type"
            + System.lineSeparator()
            + "e1; p1; 1999-01-02T01:02:03; 1999-01-12T04:05:06; t1"
            + System.lineSeparator()
            + "e2; p2; 05.06.2001; 05.07.2001; t2";

    importEncounterData(csv);

    EncounterDao enc1 =
        new EncounterDao(
            DATA_SOURCE_ID,
            "e1",
            EXPECTED_SUBJECT_1,
            "t1",
            DateUtil.parse("1999-01-02T01:02:03"),
            DateUtil.parse("1999-01-12T04:05:06"));
    EncounterDao enc2 =
        new EncounterDao(
            DATA_SOURCE_ID,
            "e2",
            EXPECTED_SUBJECT_2,
            "t2",
            DateUtil.parse("05.06.2001"),
            DateUtil.parse("05.07.2001"));

    SubjectResourceDao res1 = buildExpectedSubjectResource1(EXPECTED_SUBJECT_1, enc1);
    SubjectResourceDao res2 = buildExpectedSubjectResource2(EXPECTED_SUBJECT_2, enc2);

    assertSubjects(EXPECTED_SUBJECT_1, EXPECTED_SUBJECT_2);
    assertEncounters(enc1, enc2);
    assertSubjectResources(res1, res2);
  }

  private SubjectResourceDao buildExpectedSubjectResource1(
      SubjectDao subject, EncounterDao encounter) {
    return new SubjectResourceDao(DATA_SOURCE_ID, "r1", subject, encounter, "codesystem1", "code1")
        .dateTime(DateUtil.parse("01.02.2003"))
        .numberValue(new BigDecimal("12.34"));
  }

  private SubjectResourceDao buildExpectedSubjectResource2(
      SubjectDao subject, EncounterDao encounter) {
    return new SubjectResourceDao(DATA_SOURCE_ID, "r2", subject, encounter, "codesystem2", "code2")
        .dateTime(DateUtil.parse("2004-05-06T01:02:03"))
        .numberValue(new BigDecimal("56.78"));
  }

  private void importSubjectData(String csv) {
    SubjectCSVImport subjectCSVImport =
        new SubjectCSVImport(
            DATA_SOURCE_ID, new StringReader(csv), subjectRepository, SUBJECT_FIELDS_MAP);
    subjectCSVImport.run();
  }

  private void importSubjectResourceData(String subjectResourceCsv) {
    SubjectResourceCSVImport subjectResourceCSVImport =
        new SubjectResourceCSVImport(
            DATA_SOURCE_ID,
            new StringReader(subjectResourceCsv),
            subjectRepository,
            encounterRepository,
            subjectResourceRepository,
            SUBJECT_RESOURCE_FIELDS_MAP);
    subjectResourceCSVImport.run();
  }

  private void importEncounterData(String csv) {
    EncounterCSVImport encounterCSVImport =
        new EncounterCSVImport(
            DATA_SOURCE_ID,
            new StringReader(csv),
            subjectRepository,
            encounterRepository,
            ENCOUNTER_FIELDS_MAP);
    encounterCSVImport.run();
  }
}
