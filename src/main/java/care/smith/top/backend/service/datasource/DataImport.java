package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import java.io.Reader;
import java.util.Optional;

public abstract class DataImport {

  protected final String dataSourceId;
  protected final Reader reader;

  protected final SubjectRepository subjectRepository;
  protected final EncounterRepository encounterRepository;
  protected final SubjectResourceRepository subjectResourceRepository;

  protected DataImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository) {
    this.dataSourceId = dataSourceId;
    this.reader = reader;
    this.subjectRepository = subjectRepository;
    this.encounterRepository = encounterRepository;
    this.subjectResourceRepository = subjectResourceRepository;
  }

  public abstract void run();

  protected void saveSubject(SubjectDao subject) {
    subjectRepository.save(subject);
  }

  protected void saveEncounter(EncounterDao encounter) {
    if (encounter.getSubjectId() != null) encounter.subject(getSubject(encounter.getSubjectId()));
    encounterRepository.save(encounter);
  }

  protected void saveSubjectResource(SubjectResourceDao subjectResource) {
    SubjectDao subject = null;
    if (subjectResource.getSubjectId() != null) {
      subject = getSubject(subjectResource.getSubjectId());
      subjectResource.subject(subject);
    }

    if (subjectResource.getEncounterId() != null)
      subjectResource.encounter(getEncounter(subjectResource.getEncounterId(), subject));

    subjectResourceRepository.save(subjectResource);
  }

  private SubjectDao getSubject(String subjectId) {
    Optional<SubjectDao> subject =
        subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, subjectId);
    if (subject.isPresent()) return subject.get();
    return subjectRepository.save(new SubjectDao(dataSourceId, subjectId));
  }

  private EncounterDao getEncounter(String encounterId, SubjectDao subject) {
    Optional<EncounterDao> encounter =
        encounterRepository.findByDataSourceIdAndEncounterId(dataSourceId, encounterId);
    if (encounter.isPresent()) return encounter.get();
    if (subject == null)
      return encounterRepository.save(new EncounterDao(dataSourceId, encounterId));
    return encounterRepository.save(new EncounterDao(dataSourceId, encounterId, subject));
  }
}
