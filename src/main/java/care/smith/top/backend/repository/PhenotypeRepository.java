package care.smith.top.backend.repository;

import care.smith.top.backend.model.EntityDao;
import care.smith.top.model.DataType;
import care.smith.top.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@org.springframework.stereotype.Repository
public interface PhenotypeRepository extends EntityRepository {
  default Page<EntityDao> findAllByRepositoryIdAndTitleAndEntityTypeAndDataType(
      String repositoryId,
      String title,
      List<EntityType> entityTypes,
      DataType dataType,
      Pageable pageable) {
    return findAll(
        EntityRepository.byRepositoryId(repositoryId)
            .and(EntityRepository.byTitle(title))
            .and(EntityRepository.byEntityType(entityTypes))
            .and(EntityRepository.byDataType(dataType)),
        pageable);
  }
}
