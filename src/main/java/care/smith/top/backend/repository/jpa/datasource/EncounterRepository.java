package care.smith.top.backend.repository.jpa.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EncounterRepository
    extends JpaRepository<EncounterDao, EncounterDao.EncounterKey> {
  Optional<EncounterDao> findByDataSourceIdAndEncounterId(String dataSourceId, String encounterId);
}
