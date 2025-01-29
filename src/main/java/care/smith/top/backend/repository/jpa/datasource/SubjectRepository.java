package care.smith.top.backend.repository.jpa.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectDao, SubjectDao.SubjectKey> {
  Optional<SubjectDao> findBySubjectKeyDataSourceIdAndSubjectKeySubjectId(
      String dataSourceId, String subjectId);

  void deleteAllBySubjectKeyDataSourceId(String dataSourceId);
}
