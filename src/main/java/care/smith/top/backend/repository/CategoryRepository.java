package care.smith.top.backend.repository;

import care.smith.top.backend.model.Category;
import care.smith.top.backend.model.EntityType;

import java.util.List;

@org.springframework.stereotype.Repository
public interface CategoryRepository extends EntityBaseRepository<Category> {
  default long count() {
    return countByEntityTypeIn(new EntityType[] {EntityType.CATEGORY});
  }

  List<Category> findAllByRepositoryIdAndSuperCategories_Id(String repositoryId, String superCategoryId);
}
