package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import java.io.Reader;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public class SubjectCSVImport extends CSVImport {

  @Autowired private SubjectRepository repository;

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
    repository.save(dao);
  }
}
