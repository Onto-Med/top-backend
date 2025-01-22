package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Entity;
import care.smith.top.model.EntityType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends EntityRepository {
  default long count() {
    return countByEntityTypeIn(
        new EntityType[] {EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT});
  }

  default Map<String, Entity> getSubDependencies(
      Map<String, Entity> concepts, Map<String, Set<String>> dependencies, String repositoryId, Map<String, EntityDao> allOfRepository) {
    Set<Entity> conceptIter = concepts.values().stream().collect(Collectors.toUnmodifiableSet());
    if (allOfRepository.isEmpty()) allOfRepository.putAll(getMapOfAll(repositoryId));
    for (Entity concept : conceptIter) {
      if (ApiModelMapper.isSingleConcept(concept)) {
        if (!allOfRepository.containsKey(concept.getId())) continue;
        EntityDao entityDao = allOfRepository.get(concept.getId());
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
          concepts.putAll(getSubDependencies(children, dependencies, repositoryId, allOfRepository));
        }
      }
    }
    return concepts;
  }

  private Map<String, EntityDao> getMapOfAll(String repositoryId) {
    return findAllByRepositoryId(repositoryId, Pageable.unpaged()).stream()
        .collect(Collectors.toMap(EntityDao::getId, Function.identity()));
  }
}
