package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import java.io.Reader;
import java.util.Map;

public class EncounterCSVImport extends CSVImport {

  public EncounterCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      Map<String, String> fieldsMapping,
      char separator) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        null,
        EncounterDao.class,
        fieldsMapping,
        separator);
  }

  public EncounterCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      Map<String, String> fieldsMapping) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        encounterRepository,
        null,
        EncounterDao.class,
        fieldsMapping);
  }

  @Override
  public void run(String[] values) {
    EncounterDao encounter = new EncounterDao(dataSourceId);
    setFields(encounter, values);
    saveEncounter(encounter);
  }
}
