package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import java.io.Reader;
import java.util.Map;

public class SubjectResourceCSVImport extends CSVImport {
  private final SubjectRepository subjectRepository;
  private final SubjectResourceRepository subjectResourceRepository;
  private final EncounterRepository encounterRepository;

  public SubjectResourceCSVImport(
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader,
      char separator) {
    super(SubjectResourceDao.class, dataSourceId, fieldsMapping, reader, separator);
    this.subjectRepository = subjectRepository;
    this.encounterRepository = encounterRepository;
    this.subjectResourceRepository = subjectResourceRepository;
  }

  public SubjectResourceCSVImport(
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader) {
    super(SubjectResourceDao.class, dataSourceId, fieldsMapping, reader);
    this.subjectRepository = subjectRepository;
    this.encounterRepository = encounterRepository;
    this.subjectResourceRepository = subjectResourceRepository;
  }

  @Override
  public void run(String[] values) {
    SubjectResourceDao subjectResource = new SubjectResourceDao(dataSourceId);
    setFields(subjectResource, values);

    SubjectDao subject = null;
    if (subjectResource.getSubjectId() != null) {
      subject = getSubject(dataSourceId, subjectResource.getSubjectId(), subjectRepository);
      subjectResource.subject(subject);
    }

    if (subjectResource.getEncounterId() != null)
      subjectResource.encounter(
          getEncounter(
              dataSourceId, subjectResource.getEncounterId(), subject, encounterRepository));

    subjectResourceRepository.save(subjectResource);
  }
}
