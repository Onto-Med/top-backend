package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Concept;
import care.smith.top.model.Entity;
import care.smith.top.model.EntityType;
import care.smith.top.model.SingleConcept;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends EntityRepository {
  default long count() {
    return countByEntityTypeIn(
        new EntityType[] {EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT});
  }

  default void populateSubconcepts(List<Entity> concepts, String repositoryId) {
    for (Entity concept : concepts) {
      if (ApiModelMapper.isSingleConcept(concept)) {
        EntityDao entityDao = findByIdAndRepositoryId(concept.getId(), repositoryId).orElseThrow();
        Set<Entity> subConcepts =
            entityDao.getSubEntities().stream()
                .map(EntityDao::toApiModel)
                .collect(Collectors.toSet());
        ((SingleConcept) concept)
            .setSubConcepts(
                subConcepts.stream().map(sc -> (Concept) sc).collect(Collectors.toList()));
      }
    }
  }
}
