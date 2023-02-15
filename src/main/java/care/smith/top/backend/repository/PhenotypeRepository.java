package care.smith.top.backend.repository;

import care.smith.top.backend.model.*;
import care.smith.top.model.DataType;
import care.smith.top.model.EntityType;
import care.smith.top.model.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@org.springframework.stereotype.Repository
public interface PhenotypeRepository extends EntityRepository {
  static Specification<EntityDao> byDataType(@Nullable List<DataType> dataTypes) {
    return (root, query, cb) -> {
      if (dataTypes == null || dataTypes.isEmpty()) return cb.and();
      return root.join(EntityDao_.CURRENT_VERSION).get(EntityVersionDao_.DATA_TYPE).in(dataTypes);
    };
  }

  static Specification<EntityDao> byDataType(@Nullable DataType dataType) {
    return byDataType(dataType == null ? null : Collections.singletonList(dataType));
  }

  static Specification<EntityDao> byItemType(@Nullable List<ItemType> itemTypes) {
    return (root, query, cb) -> {
      if (itemTypes == null || itemTypes.isEmpty()) return cb.and();
      return root.join(EntityDao_.CURRENT_VERSION).get(EntityVersionDao_.ITEM_TYPE).in(itemTypes);
    };
  }

  static Specification<EntityDao> byItemType(@Nullable ItemType itemType) {
    return byItemType(itemType == null ? null : Collections.singletonList(itemType));
  }

  static Specification<EntityDao> byUser(UserDao user) {
    return (root, query, cb) -> {
      if (user == null || user.getRole().equals(Role.ADMIN)) return cb.and();
      return cb.or(
          cb.isTrue(root.join(EntityDao_.REPOSITORY).get(RepositoryDao_.PRIMARY)),
          cb.equal(
              root.join(EntityDao_.REPOSITORY)
                  .join(RepositoryDao_.ORGANISATION)
                  .join(OrganisationDao_.MEMBERS)
                  .join(OrganisationMembershipDao_.USER)
                  .get(UserDao_.ID),
              user.getId()));
    };
  }

  default Page<EntityDao> findAllByRepositoryIdAndTitleAndEntityTypeAndDataTypeAndItemType(
      String repositoryId,
      String title,
      List<EntityType> entityTypes,
      DataType dataType,
      ItemType itemType,
      Pageable pageable) {
    return findAll(
        EntityRepository.byRepositoryId(repositoryId)
            .and(EntityRepository.byTitle(title))
            .and(EntityRepository.byEntityType(entityTypes))
            .and(byDataType(dataType))
            .and(byItemType(itemType)),
        pageable);
  }

  default Page<EntityDao>
      findAllByRepositoryIdsAndRepository_PrimaryAndTitleAndEntityTypeAndDataTypeAndItemType(
          List<String> repositoryIds,
          Boolean includePrimary,
          String title,
          List<EntityType> entityTypes,
          DataType dataType,
          ItemType itemType,
          UserDao user,
          Pageable pageable) {
    return findAll(
        EntityRepository.byRepositoryIds(repositoryIds, includePrimary)
            .and(EntityRepository.byTitle(title))
            .and(EntityRepository.byEntityType(entityTypes))
            .and(byDataType(dataType))
            .and(byItemType(itemType))
            .and(byUser(user)),
        pageable);
  }
}
