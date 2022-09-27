package care.smith.top.backend.repository;

import care.smith.top.backend.model.EntityVersionDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityVersionRepository
    extends PagingAndSortingRepository<EntityVersionDao, String> {
  Optional<EntityVersionDao> findByEntityIdAndVersion(String id, Integer version);

  List<EntityVersionDao> findAllByEntity_RepositoryIdAndEntityId(
      String repositoryId, String entityId);

  Optional<EntityVersionDao> findByEntity_RepositoryIdAndEntityIdAndVersion(
      String repositoryId, String entityId, Integer version);

  EntityVersionDao findByEntityIdAndNextVersionNull(String entityId);
}
