package care.smith.top.backend.repository;

import care.smith.top.backend.model.DataType;
import care.smith.top.backend.model.EntityType;
import care.smith.top.backend.model.Phenotype;
import care.smith.top.backend.util.OntoModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@org.springframework.stereotype.Repository
public interface PhenotypeRepository extends EntityBaseRepository<Phenotype> {
  List<Phenotype> findAllByRepositoryIdAndSuperPhenotypeId(
      String repositoryId, String superPhenotypeId);

  Page<Phenotype> findAllByTitles_TextContainingIgnoreCaseAndEntityTypeInAndDataType(
      String title, List<EntityType> types, DataType dataType, Pageable pageable);

  default Page<Phenotype> findAllByTitleAndEntityTypeAndDataType(
      String title, List<EntityType> entityTypes, DataType dataType, Pageable pageable) {
    if (dataType == null) return findAllByTitleAndEntityTypes(title, entityTypes, pageable);
    if (title != null && entityTypes != null)
      return findAllByTitles_TextContainingIgnoreCaseAndEntityTypeInAndDataType(
          title, entityTypes, dataType, pageable);
    if (title != null)
      return findAllByTitles_TextContainingIgnoreCaseAndDataType(title, dataType, pageable);
    if (entityTypes != null)
      return findAllByEntityTypeInAndDataType(entityTypes, dataType, pageable);
    return findAllByDataType(dataType, pageable);
  }

  Page<Phenotype> findAllByDataType(DataType dataType, Pageable pageable);

  Page<Phenotype> findAllByEntityTypeInAndDataType(
      List<EntityType> entityTypes, DataType dataType, Pageable pageable);

  Page<Phenotype> findAllByTitles_TextContainingIgnoreCaseAndDataType(
      String title, DataType dataType, Pageable pageable);

  default Page<Phenotype> findAllByRepositoryIdAndTitleAndEntityTypeAndDataType(
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
      return findAllByRepositoryIdAndTitles_TextContainingIgnoreCaseAndEntityTypeInAndDataType(
          repositoryId, title, entityTypes, dataType, pageable);
    if (title != null)
      return findAllByRepositoryIdAndTitles_TextContainingIgnoreCaseAndDataType(
          repositoryId, title, dataType, pageable);
    if (entityTypes != null)
      return findAllByRepositoryIdAndEntityTypeInAndDataType(
          repositoryId, entityTypes, dataType, pageable);
    return findAllByRepositoryIdAndDataType(repositoryId, dataType, pageable);
  }

  Page<Phenotype> findAllByRepositoryIdAndDataType(
      String repositoryId, DataType dataType, Pageable pageable);

  Page<Phenotype> findAllByRepositoryIdAndEntityTypeInAndDataType(
      String repositoryId, List<EntityType> entityTypes, DataType dataType, Pageable pageable);

  Page<Phenotype> findAllByRepositoryIdAndTitles_TextContainingIgnoreCaseAndDataType(
      String repositoryId, String title, DataType dataType, Pageable pageable);

  Page<Phenotype> findAllByRepositoryIdAndTitles_TextContainingIgnoreCaseAndEntityTypeInAndDataType(
      String repositoryId,
      String title,
      List<EntityType> entityTypes,
      DataType dataType,
      Pageable pageable);
}
