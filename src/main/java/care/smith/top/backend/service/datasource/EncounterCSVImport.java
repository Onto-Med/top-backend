package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import java.io.Reader;
import java.util.Map;

public class EncounterCSVImport extends CSVImport {
  private final EncounterRepository encounterRepository;
  private final SubjectRepository subjectRepository;

  public EncounterCSVImport(
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader,
      char separator) {
    super(EncounterDao.class, dataSourceId, fieldsMapping, reader, separator);
    this.subjectRepository = subjectRepository;
    this.encounterRepository = encounterRepository;
  }

  public EncounterCSVImport(
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader) {
    super(EncounterDao.class, dataSourceId, fieldsMapping, reader);
    this.subjectRepository = subjectRepository;
    this.encounterRepository = encounterRepository;
  }

  @Override
  public void run(String[] values) {
    EncounterDao encounter = new EncounterDao().dataSourceId(dataSourceId);
    setFields(encounter, values);

    if (encounter.getSubjectId() != null)
      encounter.subject(getSubject(dataSourceId, encounter.getSubjectId(), subjectRepository));

    encounterRepository.save(encounter);
  }
}
