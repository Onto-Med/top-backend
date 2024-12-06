package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import java.io.Reader;
import java.util.Map;

public class SubjectCSVImport extends CSVImport {
  private final SubjectRepository subjectRepository;

  public SubjectCSVImport(
      SubjectRepository subjectRepository,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader,
      char separator) {
    super(SubjectDao.class, dataSourceId, fieldsMapping, reader, separator);
    this.subjectRepository = subjectRepository;
  }

  public SubjectCSVImport(
      SubjectRepository subjectRepository,
      String dataSourceId,
      Map<String, String> fieldsMapping,
      Reader reader) {
    super(SubjectDao.class, dataSourceId, fieldsMapping, reader);
    this.subjectRepository = subjectRepository;
  }

  @Override
  public void run(String[] values) {
    SubjectDao subject = new SubjectDao().dataSourceId(dataSourceId);
    setFields(subject, values);
    subjectRepository.save(subject);
  }
}
