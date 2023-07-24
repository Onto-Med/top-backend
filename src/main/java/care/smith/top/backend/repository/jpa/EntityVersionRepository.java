package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.EntityVersionDao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityVersionRepository extends JpaRepository<EntityVersionDao, String> {
  List<EntityVersionDao> findAllByEntity_RepositoryIdAndEntityIdOrderByVersionDesc(
      String repositoryId, String entityId);

  Optional<EntityVersionDao> findByEntity_RepositoryIdAndEntityIdAndVersion(
      String repositoryId, String entityId, Integer version);

  EntityVersionDao findByEntityIdAndNextVersionNull(String entityId);
}
