package care.smith.top.backend.repository;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.EntityType;
import care.smith.top.backend.model.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.Optional;

@NoRepositoryBean
public interface EntityBaseRepository<T extends Entity> extends PagingAndSortingRepository<T, String> {
  long count();

  long countByEntityTypeIn(EntityType[] entityType);

  Collection<Entity> findAllById(String id, PageRequest pageRequest);

  Optional<Entity> findByIdAndVersion(String id, Integer version);

  default Optional<Entity> findCurrentById(String id) {
    return Optional.empty();
  }

//  Collection<Entity> findAllByRepositoryIdAndNameAndEntityTypeAndDataTypeAndPrimary(
//      String repositoryId,
//      String name,
//      List<EntityType> type,
//      DataType dataType,
//      boolean primary,
//      PageRequest pageRequest);

  default Optional<Entity> getFork(Entity origin, Repository repository) {
    return origin.getForks().stream()
        .filter(e -> e.getRepository().equals(repository) && e.getNextVersion() == null)
        .findFirst();
  }

  default void setFork(String forkId, String originId) {

  }

  Optional<Entity> findByIdAndRepositoryId(String id, String repositoryId);
}
