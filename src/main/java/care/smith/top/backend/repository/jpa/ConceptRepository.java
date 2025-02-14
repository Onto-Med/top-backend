package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.model.Entity;
import care.smith.top.model.EntityType;
import java.util.*;
import java.util.stream.Collectors;
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
  List<EntityDao> getEntityTreeByEntityId(String entityId, Integer levelCap); //, String levelCap

  default void populateEntities(
      Map<String, Entity> concepts, Map<String, Set<String>> dependencies, int levelCap) {
    Set<String> conceptIter = concepts.keySet().stream().collect(Collectors.toUnmodifiableSet());
    for (String conceptId : conceptIter) {
      for (EntityDao entity : getEntityTreeByEntityId(conceptId, levelCap)) { // , Integer.toString(levelCap)
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
}
