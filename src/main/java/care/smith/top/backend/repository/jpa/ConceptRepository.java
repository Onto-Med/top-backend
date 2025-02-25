package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.model.jpa.UserDao;
import care.smith.top.model.Entity;
import care.smith.top.model.EntityType;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends EntityRepository {
  default long count() {
    return countByEntityTypeIn(
        new EntityType[] {EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT});
  }

  @Query(
      nativeQuery = true,
      value =
          "WITH RECURSIVE tree AS ("
              + " SELECT *, NULL\\:\\:character varying AS parent_id, 0 AS level"
              + " FROM entity e"
              + " WHERE id = :entityId"
              + " UNION"
              + " SELECT entity.*, super_entities_id AS parent_id, level + 1 AS level"
              + " FROM entity"
              + "   JOIN entity_super_entities ON (id = sub_entities_id)"
              + "   JOIN tree t ON (t.id = super_entities_id)"
              + "   WHERE level < :levelCap"
              + ") SELECT * FROM tree")
  List<EntityDao> getEntityTreeByEntityId(String entityId, Integer levelCap);

  @Query(
      nativeQuery = true,
      value =
          "WITH RECURSIVE tree AS ("
              + " SELECT *, NULL\\:\\:character varying AS parent_id, 0 AS level"
              + " FROM entity e"
              + " WHERE id = :entityId"
              + " UNION"
              + " SELECT entity.*, super_entities_id AS parent_id, level + 1 AS level"
              + " FROM entity"
              + "   JOIN entity_super_entities ON (id = sub_entities_id)"
              + "   JOIN tree t ON (t.id = super_entities_id)"
              + ") SELECT * FROM tree")
  List<EntityDao> getEntityTreeByEntityId(String entityId);

  /**
   * If the depth of an entity
   *
   * <ul>
   *   <li>== 0 -> subConcepts won't be resolved.
   *   <li>> 0 -> subConcepts will be resolved up until and including the depth
   *   <li>< 0 -> all subConcepts will be resolved
   * </ul>
   *
   * @param concepts
   * @param dependencies
   * @param depthMap
   */
  default void populateEntities(
      Map<String, Entity> concepts,
      Map<String, Set<String>> dependencies,
      Map<String, Integer> depthMap) {
    Set<String> conceptIter = concepts.keySet().stream().collect(Collectors.toUnmodifiableSet());
    for (String conceptId : conceptIter) {
      if (!depthMap.containsKey(conceptId)) continue;
      Integer depth = depthMap.get(conceptId);
      if (depth == 0) continue;

      List<EntityDao> entities =
          depth > 0
              ? getEntityTreeByEntityId(conceptId, depthMap.get(conceptId))
              : getEntityTreeByEntityId(conceptId);
      for (EntityDao entity : entities) {
        String entityId = entity.getId();
        if (!concepts.containsKey(entityId)) {
          concepts.put(entityId, entity.toApiModel());
        }
        if (dependencies.containsKey(entityId)) {
          dependencies
              .get(entityId)
              .addAll(
                  entity.getSubEntities().stream()
                      .map(EntityDao::getId)
                      .collect(Collectors.toUnmodifiableSet()));
        } else {
          dependencies.put(
              entityId,
              entity.getSubEntities().stream()
                  .map(EntityDao::getId)
                  .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
      }
    }
  }

  default Page<EntityDao> findAllByRepositoryIdsAndRepository_PrimaryAndTitleAndEntityType(
      List<String> repositoryIds,
      Boolean includePrimary,
      String title,
      List<EntityType> entityTypes,
      UserDao user,
      Pageable pageable) {
    return findAll(
        EntityRepository.byRepositoryIds(repositoryIds, includePrimary)
            .and(EntityRepository.byTitle(title))
            .and(EntityRepository.byEntityType(entityTypes))
            .and(EntityRepository.byUser(user)),
        pageable);
  }
}
