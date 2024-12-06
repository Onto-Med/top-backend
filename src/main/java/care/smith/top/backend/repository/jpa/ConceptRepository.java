package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Entity;
import care.smith.top.model.EntityType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends EntityRepository {
  default long count() {
    return countByEntityTypeIn(
        new EntityType[] {EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT});
  }

  default Map<String, Entity> getSubDependencies(
      Map<String, Entity> concepts, Map<String, Set<String>> dependencies, String repositoryId) {
    Set<Entity> conceptIter = concepts.values().stream().collect(Collectors.toUnmodifiableSet());
    for (Entity concept : conceptIter) {
      if (ApiModelMapper.isSingleConcept(concept)) {
        EntityDao entityDao = findByIdAndRepositoryId(concept.getId(), repositoryId).orElseThrow();
        Map<String, Entity> children =
            entityDao.getSubEntities().stream()
                .map(EntityDao::toApiModel)
                .collect(Collectors.toMap(Entity::getId, Function.identity()));
        if (!children.isEmpty()) {
          if (dependencies.containsKey(concept.getId())) {
            dependencies.get(concept.getId()).addAll(children.keySet());
          } else {
            dependencies.put(concept.getId(), new HashSet<>(children.keySet()));
          }
          concepts.putAll(getSubDependencies(children, dependencies, repositoryId));
        }
      }
    }
    return concepts;
  }
}
