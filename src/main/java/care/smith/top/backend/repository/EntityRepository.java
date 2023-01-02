package care.smith.top.backend.repository;

import care.smith.top.backend.model.*;
import care.smith.top.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.criteria.JoinType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntityRepository
    extends JpaRepository<EntityDao, String>, JpaSpecificationExecutor<EntityDao> {
  long count();

  long countByEntityTypeIn(EntityType[] entityType);

  static Specification<EntityDao> byTitle(@Nullable String title) {
    return (root, query, cb) -> {
      if (title == null) return cb.and();
      String pattern = "%" + title.toLowerCase() + "%";
      return cb.or(
          cb.like(
              cb.lower(
                  root.join(EntityDao_.CURRENT_VERSION)
                      .join(EntityVersionDao_.TITLES)
                      .get(LocalisableTextDao_.TEXT)),
              pattern),
          cb.and(
              root.get(EntityDao_.ENTITY_TYPE)
                  .in(EntityType.COMPOSITE_RESTRICTION, EntityType.SINGLE_RESTRICTION),
              cb.like(
                  cb.lower(
                      root.join(EntityDao_.SUPER_ENTITIES, JoinType.LEFT)
                          .join(EntityDao_.CURRENT_VERSION, JoinType.LEFT)
                          .join(EntityVersionDao_.TITLES, JoinType.LEFT)
                          .get(LocalisableTextDao_.TEXT)),
                  pattern)));
    };
  }

  static Specification<EntityDao> byEntityType(@Nullable List<EntityType> entityTypes) {
    return (root, query, cb) -> {
      if (entityTypes == null || entityTypes.isEmpty()) return cb.and();
      return root.get(EntityDao_.ENTITY_TYPE).in(entityTypes);
    };
  }

  static Specification<EntityDao> byEntityType(@Nullable EntityType entityType) {
    return byEntityType(entityType == null ? null : Collections.singletonList(entityType));
  }

  static Specification<EntityDao> byRepositoryId(@Nullable String repositoryId) {
    return (root, query, cb) -> {
      if (repositoryId == null) return cb.and();
      return cb.equal(root.join(EntityDao_.REPOSITORY).get(RepositoryDao_.ID), repositoryId);
    };
  }

  List<EntityDao> findAllByRepositoryIdAndSuperEntities_Id(
      String repositoryId, String superCategoryId);

  default Optional<EntityDao> getFork(EntityDao origin, RepositoryDao repository) {
    return findByOriginAndRepository(origin, repository);
  }

  Optional<EntityDao> findByOriginAndRepository(EntityDao origin, RepositoryDao repository);

  default void setFork(String forkId, String originId) {}

  Optional<EntityDao> findByIdAndRepositoryId(String id, String repositoryId);

  Page<EntityDao> findAllByRepositoryId(String repositoryId, Pageable pageable);

  Slice<EntityDao> findAllByRepositoryIdAndSuperEntitiesEmpty(
      String repositoryId, Pageable pageable);

  Optional<EntityDao> findByRepositoryIdAndOriginId(String repositoryId, String originId);

  Page<EntityDao> findAllByRepositoryIdAndEntityTypeIn(
      String repositoryId, List<EntityType> entityTypes, Pageable pageable);
}
