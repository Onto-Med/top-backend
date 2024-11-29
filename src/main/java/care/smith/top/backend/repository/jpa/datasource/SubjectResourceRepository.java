package care.smith.top.backend.repository.jpa.datasource;

import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectResourceRepository
    extends JpaRepository<SubjectResourceDao, SubjectResourceDao.SubjectResourceKey> {}
