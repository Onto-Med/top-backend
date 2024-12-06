package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;

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
    SubjectResourceDao dao = new SubjectResourceDao().dataSourceId(dataSourceId);
    setFields(dao, values);

    if (dao.getSubjectId() != null) {
      Optional<SubjectDao> subject =
          subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, dao.getSubjectId());
      if (subject.isPresent()) dao.subject(subject.get());
    }

    if (dao.getEncounterId() != null) {
      Optional<EncounterDao> encounter =
          encounterRepository.findByDataSourceIdAndEncounterId(dataSourceId, dao.getEncounterId());
      if (encounter.isPresent()) dao.encounter(encounter.get());
    }

    subjectResourceRepository.save(dao);
  }
}
