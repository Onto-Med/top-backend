package care.smith.top.backend.repository;

import care.smith.top.backend.model.EntityDao;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.DataType;
import care.smith.top.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@org.springframework.stereotype.Repository
public interface PhenotypeRepository extends EntityRepository {
  default List<EntityDao> findAllByRepositoryIdAndSuperPhenotypeId(
      String repositoryId, String superPhenotypeId) {
    return findAllByRepositoryIdAndSuperEntities_IdAndEntityType(
        repositoryId, superPhenotypeId, ApiModelMapper.phenotypeTypes());
  }

  Page<EntityDao>
      findAllByCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeInAndCurrentVersion_DataType(
          String title, List<EntityType> types, DataType dataType, Pageable pageable);

  default Page<EntityDao> findAllByTitleAndEntityTypeAndDataType(
      String title, List<EntityType> entityTypes, DataType dataType, Pageable pageable) {
    if (dataType == null) return findAllByTitleAndEntityTypes(title, entityTypes, pageable);
    if (title != null && entityTypes != null)
      return findAllByCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeInAndCurrentVersion_DataType(
          title, entityTypes, dataType, pageable);
    if (title != null)
      return findAllByCurrentVersion_Titles_TextContainingIgnoreCaseAndCurrentVersion_DataType(
          title, dataType, pageable);
    if (entityTypes != null)
      return findAllByEntityTypeInAndCurrentVersion_DataType(entityTypes, dataType, pageable);
    return findAllByCurrentVersion_DataType(dataType, pageable);
  }

  Page<EntityDao> findAllByCurrentVersion_DataType(DataType dataType, Pageable pageable);

  Page<EntityDao> findAllByEntityTypeInAndCurrentVersion_DataType(
      List<EntityType> entityTypes, DataType dataType, Pageable pageable);

  Page<EntityDao> findAllByCurrentVersion_Titles_TextContainingIgnoreCaseAndCurrentVersion_DataType(
      String title, DataType dataType, Pageable pageable);

  default Page<EntityDao> findAllByRepositoryIdAndTitleAndEntityTypeAndDataType(
      String repositoryId,
      String title,
      List<EntityType> entityTypes,
      DataType dataType,
      Pageable pageable) {
    if (repositoryId == null)
      return findAllByTitleAndEntityTypeAndDataType(title, entityTypes, dataType, pageable);
    if (dataType == null)
      return findAllByRepositoryIdAndTitleAndEntityTypes(
          repositoryId, title, entityTypes, pageable);
    if (title != null && entityTypes != null)
      return findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeInAndCurrentVersion_DataType(
          repositoryId, title, entityTypes, dataType, pageable);
    if (title != null)
      return findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCaseAndCurrentVersion_DataType(
          repositoryId, title, dataType, pageable);
    if (entityTypes != null)
      return findAllByRepositoryIdAndEntityTypeInAndCurrentVersion_DataType(
          repositoryId, entityTypes, dataType, pageable);
    return findAllByRepositoryIdAndCurrentVersion_DataType(repositoryId, dataType, pageable);
  }

  Page<EntityDao> findAllByRepositoryIdAndCurrentVersion_DataType(
      String repositoryId, DataType dataType, Pageable pageable);

  Page<EntityDao> findAllByRepositoryIdAndEntityTypeInAndCurrentVersion_DataType(
      String repositoryId, List<EntityType> entityTypes, DataType dataType, Pageable pageable);

  Page<EntityDao>
      findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCaseAndCurrentVersion_DataType(
          String repositoryId, String title, DataType dataType, Pageable pageable);

  Page<EntityDao>
      findAllByRepositoryIdAndCurrentVersion_Titles_TextContainingIgnoreCaseAndEntityTypeInAndCurrentVersion_DataType(
          String repositoryId,
          String title,
          List<EntityType> entityTypes,
          DataType dataType,
          Pageable pageable);
}
