package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EncounterCSVImport extends CSVImport {

  public EncounterCSVImport(
      String dataSourceId, Map<String, String> fieldsMapping, Reader reader, char separator) {
    super(EncounterDao.class, dataSourceId, fieldsMapping, reader, separator);
  }

  public EncounterCSVImport(String dataSourceId, Map<String, String> fieldsMapping, Reader reader) {
    super(EncounterDao.class, dataSourceId, fieldsMapping, reader);
  }

  @Override
  public void run(String[] values) {
    EncounterDao dao = new EncounterDao().dataSourceId(dataSourceId);
    setFields(dao, values);

    if (dao.getSubjectId() != null) {
      Optional<SubjectDao> subject =
          subjectRepository.findByDataSourceIdAndSubjectId(dataSourceId, dao.getSubjectId());
      if (subject.isPresent()) dao.subject(subject.get());
    }

    encounterRepository.save(dao);
  }
}
