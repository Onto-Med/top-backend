package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SubjectResourceCSVImport extends CSVImport {

  public SubjectResourceCSVImport(
      String dataSourceId, Map<String, String> fieldsMapping, Reader reader, char separator) {
    super(SubjectResourceDao.class, dataSourceId, fieldsMapping, reader, separator);
  }

  public SubjectResourceCSVImport(
      String dataSourceId, Map<String, String> fieldsMapping, Reader reader) {
    super(SubjectResourceDao.class, dataSourceId, fieldsMapping, reader);
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
