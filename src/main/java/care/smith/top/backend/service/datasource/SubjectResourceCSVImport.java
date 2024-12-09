package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import java.io.Reader;
import java.util.Map;

public class SubjectResourceCSVImport extends CSVImport {

  public SubjectResourceCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      Map<String, String> fieldsMapping,
      char separator) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        SubjectResourceDao.class,
        fieldsMapping,
        separator);
  }

  public SubjectResourceCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      Map<String, String> fieldsMapping) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        subjectResourceRepository,
        SubjectResourceDao.class,
        fieldsMapping);
  }

  @Override
  public void run(String[] values) {
    SubjectResourceDao subjectResource = new SubjectResourceDao(dataSourceId);
    setFields(subjectResource, values);
    saveSubjectResource(subjectResource);
  }
}
