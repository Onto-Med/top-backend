package care.smith.top.backend.repository;

import care.smith.top.backend.model.EntityVersionDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityVersionRepository extends JpaRepository<EntityVersionDao, String> {
  List<EntityVersionDao> findAllByEntity_RepositoryIdAndEntityIdOrderByVersionDesc(
      String repositoryId, String entityId);

  Optional<EntityVersionDao> findByEntity_RepositoryIdAndEntityIdAndVersion(
      String repositoryId, String entityId, Integer version);

  EntityVersionDao findByEntityIdAndNextVersionNull(String entityId);
}
