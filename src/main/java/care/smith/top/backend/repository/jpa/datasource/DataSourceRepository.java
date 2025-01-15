package care.smith.top.backend.repository.jpa.datasource;

import care.smith.top.backend.model.jpa.datasource.DataSourceDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceDao, String> {}
