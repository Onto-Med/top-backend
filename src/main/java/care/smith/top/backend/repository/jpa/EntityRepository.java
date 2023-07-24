package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.EntityType;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityRepository
    extends JpaRepository<EntityDao, String>, JpaSpecificationExecutor<EntityDao> {
  static Specification<EntityDao> byTitle(@Nullable String title) {
    return (root, query, cb) -> {
      if (title == null) return cb.and();
      String pattern = "%" + title.toLowerCase() + "%";
      query.distinct(true); // This may cause side effects!
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

  static Specification<EntityDao> byUser(UserDao user) {
    return (root, query, cb) -> {
      if (user == null || user.getRole().equals(Role.ADMIN)) return cb.and();
      query.distinct(true); // This may cause side effects!
      return cb.or(
          cb.isTrue(root.join(EntityDao_.REPOSITORY).get(RepositoryDao_.PRIMARY)),
          cb.equal(
              root.join(EntityDao_.REPOSITORY)
                  .join(RepositoryDao_.ORGANISATION)
                  .join(OrganisationDao_.MEMBERS, JoinType.LEFT)
                  .join(OrganisationMembershipDao_.USER, JoinType.LEFT)
                  .get(UserDao_.ID),
              user.getId()));
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

  /**
   * This filter does the following:
   *
   * <ul>
   *   <li>exclude entities that are not contained in one of the specified repositories
   *   <li>if you set {@code includePrimary} to {@code true}, this filter will be relaxed and will
   *       not exclude entities from primary repositories
   *   <li>if {@code repositoryIds} is {@code null}, this filter does nothing
   * </ul>
   *
   * @param repositoryIds List of repository IDs to folter for, or null to disable this filter.
   * @param includePrimary Whether primary repositories shall be included in the result set. This
   *     parameter has no effect, if {@code repositoryIds} is {@code null}.
   * @return A specification for Domain Driven Design.
   */
  static Specification<EntityDao> byRepositoryIds(
      List<String> repositoryIds, Boolean includePrimary) {
    return (root, query, cb) -> {
      if (repositoryIds == null) return cb.and();
      Predicate repositoryPredicate =
          root.join(EntityDao_.REPOSITORY).get(RepositoryDao_.ID).in(repositoryIds);
      return cb.or(
          repositoryPredicate,
          (includePrimary != null && includePrimary)
              ? cb.isTrue(root.join(EntityDao_.REPOSITORY).get(RepositoryDao_.PRIMARY))
              : cb.or());
    };
  }

  /**
   * This method collects all dependencies of an entity.
   *
   * <p>Dependencies can be:
   *
   * <ul>
   *   <li>super phenotypes of restricted phenotypes
   *   <li>entities referenced from expressions
   * </ul>
   *
   * @param entity The entity to collect dependencies for.
   * @return A set of {@link EntityDao} objects.
   */
  default Set<EntityDao> getDependencies(EntityDao entity) {
    Set<EntityDao> dependencies = new HashSet<>();

    if (ApiModelMapper.isRestricted(entity.getEntityType())) {
      entity.getSuperEntities().stream().findFirst().ifPresent(dependencies::add);
    }

    if (ApiModelMapper.isAbstract(entity.getEntityType())
        || ApiModelMapper.isCompositeConcept(entity.getEntityType())) {
      EntityVersionDao currentVersion = entity.getCurrentVersion();
      if (currentVersion != null && currentVersion.getExpression() != null) {
        ExpressionDao expression = entity.getCurrentVersion().getExpression();
        dependencies.addAll(
            ApiModelMapper.getEntityIdsFromExpression(expression.toApiModel()).stream()
                .distinct()
                .map(this::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet()));
      }
    }

    Set<EntityDao> superDependencies = new HashSet<>();
    dependencies.forEach(d -> superDependencies.addAll(getDependencies(d)));
    dependencies.addAll(superDependencies);

    return dependencies;
  }

  long count();

  long countByEntityTypeIn(EntityType[] entityType);

  boolean existsByIdAndSubEntities_EntityTypeIn(String id, Collection<EntityType> entityTypes);

  Slice<EntityDao> findAllByRepositoryIdAndSuperEntities_Id(
      String repositoryId, String superCategoryId, Sort sort);

  default Optional<EntityDao> getFork(EntityDao origin, RepositoryDao repository) {
    return findByOriginAndRepository(origin, repository);
  }

  Optional<EntityDao> findByOriginAndRepository(EntityDao origin, RepositoryDao repository);

  default void setFork(String forkId, String originId) {}

  Optional<EntityDao> findByIdAndRepositoryId(String id, String repositoryId);

  Page<EntityDao> findAllByRepositoryId(String repositoryId, Pageable pageable);

  Slice<EntityDao> findAllByRepositoryIdAndSuperEntitiesEmpty(String repositoryId, Sort sort);

  Optional<EntityDao> findByRepositoryIdAndOriginId(String repositoryId, String originId);

  Page<EntityDao> findAllByRepositoryIdAndEntityTypeIn(
      String repositoryId, List<EntityType> entityTypes, Pageable pageable);
}
