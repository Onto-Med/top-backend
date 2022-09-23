package care.smith.top.backend.repository;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.EntityType;
import care.smith.top.backend.model.Repository;
import io.swagger.v3.core.util.AnnotationsUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface EntityBaseRepository<T extends Entity>
    extends PagingAndSortingRepository<T, String> {
  long count();

  long countByEntityTypeIn(EntityType[] entityType);

  Collection<T> findAllById(String id, PageRequest pageRequest);

  Optional<T> findByIdAndVersion(String id, Integer version);

  default Optional<T> findCurrentById(String id) {
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

  default void setFork(String forkId, String originId) {}

  Optional<T> findByIdAndRepositoryId(String id, String repositoryId);

  Page<T> findAllByTitles_TextContainingIgnoreCaseAndEntityTypeIn(
      String title, List<EntityType> entityTypes, Pageable pageable);

  Page<T> findAllByTitles_TextContainingIgnoreCase(String title, Pageable pageable);

  default Page<T> findAllByTitleAndEntityTypes(
      String title, List<EntityType> entityTypes, Pageable pageable) {
    if (title != null && entityTypes != null)
      return findAllByTitles_TextContainingIgnoreCaseAndEntityTypeIn(title, entityTypes, pageable);
    if (title != null) return findAllByTitles_TextContainingIgnoreCase(title, pageable);
    if (entityTypes != null) return findAllByEntityTypeIn(entityTypes, pageable);
    return findAll(pageable);
  }

  Page<T> findAllByEntityTypeIn(List<EntityType> entityTypes, Pageable pageable);

  Page<T> findAllByRepositoryIdAndTitleAndEntityTypes(
      String repositoryId, String title, List<EntityType> entityTypes, Pageable pageable);
}
