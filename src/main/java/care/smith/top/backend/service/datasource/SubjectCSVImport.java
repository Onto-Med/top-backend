package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import java.io.Reader;
import java.util.Map;

public class SubjectCSVImport extends CSVImport {

  public SubjectCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      Map<String, String> fieldsMapping,
      char separator) {
    super(
        dataSourceId,
        reader,
        subjectRepository,
        null,
        null,
        SubjectDao.class,
        fieldsMapping,
        separator);
  }

  public SubjectCSVImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      Map<String, String> fieldsMapping) {
    super(dataSourceId, reader, subjectRepository, null, null, SubjectDao.class, fieldsMapping);
  }

  @Override
  public void run(String[] values) {
    SubjectDao subject = new SubjectDao(dataSourceId, null);
    setFields(subject, values);
    saveSubject(subject);
  }
}
