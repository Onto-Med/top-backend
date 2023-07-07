package care.smith.top.backend.repository;

import care.smith.top.model.EntityType;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends EntityRepository {
    default long count() {
        return countByEntityTypeIn(new EntityType[] {EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT});
    }
}
