package care.smith.top.backend.repository;

import care.smith.top.backend.model.EntityDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityRepository extends JpaRepository<EntityDao, String> {
  long count();

  long countByEntityTypeIn(EntityType[] entityType);

  Optional<EntityDao> findById(String id, PageRequest pageRequest);

  Optional<EntityDao> findByIdAndCurrentVersion_Version(String id, Integer version);

  List<EntityDao> findAllByRepositoryIdAndSuperEntities_IdAndEntityTypeIn(
      String repositoryId, String superPhenotypeId, List<EntityType> entityTypes);

  List<EntityDao> findAllByRepositoryIdAndSuperEntities_Id(
      String repositoryId, String superCategoryId);

  default Optional<EntityDao> getFork(EntityDao origin, RepositoryDao repository) {
    return findByOriginAndRepository(origin, repository);
  }

  Optional<EntityDao> findByOriginAndRepository(EntityDao origin, RepositoryDao repository);

  default void setFork(String forkId, String originId) {}

  Optional<EntityDao> findByIdAndRepositoryId(String id, String repositoryId);

  Page<EntityDao> findAllByCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeIn(
      String title, List<EntityType> entityTypes, Pageable pageable);

  Page<EntityDao> findAllByCurrentVersion_Titles_TextContainingIgnoreCase(
      String title, Pageable pageable);

  default Page<EntityDao> findAllByTitleAndEntityTypes(
      String title, List<EntityType> entityTypes, Pageable pageable) {
    if (title != null && entityTypes != null)
      return findAllByCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeIn(
          title, entityTypes, pageable);
    if (title != null)
      return findAllByCurrentVersion_Titles_TextContainingIgnoreCase(title, pageable);
    if (entityTypes != null) return findAllByEntityTypeIn(entityTypes, pageable);
    return findAll(pageable);
  }

  Page<EntityDao> findAllByEntityTypeIn(List<EntityType> entityTypes, Pageable pageable);

  default Page<EntityDao> findAllByRepositoryIdAndTitleAndEntityTypes(
      String repositoryId, String title, List<EntityType> entityTypes, Pageable pageable) {
    if (repositoryId == null) return findAllByTitleAndEntityTypes(title, entityTypes, pageable);
    if (title != null && entityTypes != null)
      return findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeIn(
          repositoryId, title, entityTypes, pageable);
    if (title != null)
      return findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCase(
          repositoryId, title, pageable);
    if (entityTypes != null)
      return findAllByRepositoryIdAndEntityTypeIn(repositoryId, entityTypes, pageable);
    return findAllByRepositoryId(repositoryId, pageable);
  }

  Page<EntityDao> findAllByRepositoryId(String repositoryId, Pageable pageable);

  Page<EntityDao> findAllByRepositoryIdAndEntityTypeIn(
      String repositoryId, List<EntityType> entityTypes, Pageable pageable);

  Page<EntityDao> findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCase(
      String repositoryId, String title, Pageable pageable);

  Page<EntityDao>
      findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeIn(
          String repositoryId, String title, List<EntityType> entityTypes, Pageable pageable);

  Slice<EntityDao> findAllByRepositoryIdAndSuperEntitiesEmpty(
      String repositoryId, Pageable pageable);
}
