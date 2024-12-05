package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import java.io.Reader;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SubjectCSVImport extends CSVImport {

  public SubjectCSVImport(
      String dataSourceId, Map<String, String> fieldsMapping, Reader reader, char separator) {
    super(SubjectDao.class, dataSourceId, fieldsMapping, reader, separator);
  }

  public SubjectCSVImport(String dataSourceId, Map<String, String> fieldsMapping, Reader reader) {
    super(SubjectDao.class, dataSourceId, fieldsMapping, reader);
  }

  @Override
  public void run(String[] values) {
    SubjectDao dao = new SubjectDao().dataSourceId(dataSourceId);
    setFields(dao, values);
    subjectRepository.save(dao);
  }
}
