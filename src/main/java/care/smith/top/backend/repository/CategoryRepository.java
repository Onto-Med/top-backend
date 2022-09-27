package care.smith.top.backend.repository;

import care.smith.top.model.EntityType;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends EntityRepository {
  default long count() {
    return countByEntityTypeIn(new EntityType[] {EntityType.CATEGORY});
  }
}
