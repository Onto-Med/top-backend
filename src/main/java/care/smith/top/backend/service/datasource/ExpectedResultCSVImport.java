package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.ExpectedResultDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.ExpectedResultRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import java.io.Reader;
import java.util.Map;

public class ExpectedResultCSVImport extends CSVImport {

  protected ExpectedResultCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      ExpectedResultRepository expectedResultRepository,
      Map<String, String> fieldsMapping,
      char separator) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        expectedResultRepository,
        ExpectedResultDao.class,
        fieldsMapping,
        separator);
  }

  protected ExpectedResultCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      ExpectedResultRepository expectedResultRepository,
      Map<String, String> fieldsMapping) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        expectedResultRepository,
        ExpectedResultDao.class,
        fieldsMapping);
  }

  @Override
  public void run(String[] values) {
    ExpectedResultDao expectedResult = new ExpectedResultDao(dataSourceId, null);
    setFields(expectedResult, values);
    saveExpectedResult(expectedResult);
  }
}
