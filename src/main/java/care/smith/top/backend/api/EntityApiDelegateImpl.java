package care.smith.top.backend.api;

import static care.smith.top.backend.configuration.RequestValidator.isValidId;

import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import care.smith.top.top_document_query.SONG;
import care.smith.top.top_phenotypic_query.c2reasoner.C2R;
import care.smith.top.top_phenotypic_query.c2reasoner.constants.ConstantEntity;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.FunctionEntity;
import care.smith.top.top_phenotypic_query.converter.PhenotypeExporter;
import care.smith.top.top_phenotypic_query.converter.PhenotypeImporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EntityApiDelegateImpl implements EntityApiDelegate {
  private final C2R c2r = new C2R();

  @Autowired EntityService entityService;

  @Override
  public ResponseEntity<Entity> createEntity(
      String organisationId, String repositoryId, Entity entity, List<String> include) {
    if (!isValidId(entity.getId()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The provided entity ID was invalid.");
    return new ResponseEntity<>(
        entityService.createEntity(organisationId, repositoryId, entity), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> bulkUploadEntities(
      String organisationId, String repositoryId, List<Entity> entities, List<String> include) {
    entityService.createEntities(organisationId, repositoryId, entities, include);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Entity> getEntityById(
      String organisationId,
      String repositoryId,
      String id,
      Integer version,
      List<String> include) {
    return new ResponseEntity<>(
        entityService.loadEntity(organisationId, repositoryId, id, version), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteEntityById(
      String organisationId,
      String repositoryId,
      String id,
      Integer version,
      List<String> include,
      EntityDeleteOptions entityDeleteOptions) {
    if (version != null) {
      entityService.deleteVersion(organisationId, repositoryId, id, version);
    } else {
      entityService.deleteEntity(
          organisationId,
          repositoryId,
          id,
          entityDeleteOptions != null ? entityDeleteOptions.isCascade() : false);
    }
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Entity> updateEntityById(
      String organisationId,
      String repositoryId,
      String id,
      Entity entity,
      Integer version,
      List<String> include) {
    return new ResponseEntity<>(
        entityService.updateEntityById(organisationId, repositoryId, id, entity, include),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<EntityPage> getEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toEntityPage(
            entityService.getEntitiesByRepositoryId(
                organisationId, repositoryId, include, name, type, dataType, itemType, page)));
  }

  @Override
  public ResponseEntity<EntityPage> getEntities(
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      List<String> repositoryIds,
      Boolean includePrimary,
      Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toEntityPage(
            entityService.getEntities(
                include, name, type, dataType, itemType, repositoryIds, includePrimary, page)));
  }

  @Override
  public ResponseEntity<List<Entity>> getRootEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType) {
    return ResponseEntity.ok(
        entityService.getRootEntitiesByRepositoryId(
            organisationId, repositoryId, include, name, type, dataType, itemType));
  }

  @Override
  public ResponseEntity<List<Entity>> getEntityVersionsById(
      String organisationId, String repositoryId, String id, List<String> include) {
    return new ResponseEntity<>(
        entityService.getVersions(organisationId, repositoryId, id, include), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<Entity>> getSubclassesById(
      String organisationId, String repositoryId, String id, List<String> include) {
    return new ResponseEntity<>(
        entityService.getSubclasses(organisationId, repositoryId, id, include), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Entity> setCurrentEntityVersion(
      String organisationId,
      String repositoryId,
      String id,
      Integer version,
      List<String> include) {
    return new ResponseEntity<>(
        entityService.setCurrentEntityVersion(organisationId, repositoryId, id, version, include),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<Entity>> createFork(
      String organisationId,
      String repositoryId,
      String id,
      ForkingInstruction forkingInstruction,
      List<String> include,
      Integer version) {
    return new ResponseEntity<>(
        entityService.createFork(
            organisationId, repositoryId, id, forkingInstruction, version, include),
        HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<ForkingStats> getForks(
      String organisationId, String repositoryId, String id, List<String> include) {
    return new ResponseEntity<>(
        entityService.getForkingStats(organisationId, repositoryId, id, include), HttpStatus.OK);
  }

  @Override
  @Cacheable("expressionFunctions")
  public ResponseEntity<List<ExpressionFunction>> getExpressionFunctions(String type) {
    return new ResponseEntity<>(
        Stream.concat(
                c2r.getFunctions().stream().map(FunctionEntity::getFunction),
                Arrays.stream(SONG.getExpressionFunctions()).map(f -> f.type("textFunction")))
            .filter(f -> StringUtils.isBlank(type) || type.equals(f.getType()))
            .sorted(
                Comparator.comparing(ExpressionFunction::getType)
                    .thenComparing(ExpressionFunction::getTitle))
            .collect(Collectors.toList()),
        HttpStatus.OK);
  }

  @Override
  @Cacheable("converters")
  public ResponseEntity<List<Converter>> getConverters(Purpose purpose) {
    List<Converter> formats = new ArrayList<>();

    if (purpose == null || purpose.equals(Purpose.IMPORT)) {
      formats.addAll(
          entityService.getPhenotypeImporterImplementations().stream()
              .map(
                  c -> {
                    Converter format =
                        new Converter().id(c.getSimpleName()).purpose(Purpose.IMPORT);
                    try {
                      PhenotypeImporter instance = c.getConstructor().newInstance();
                      format.setFileExtension(instance.getFileExtension());
                    } catch (Exception ignored) {
                    }
                    return format;
                  })
              .collect(Collectors.toList()));
    }

    if (purpose == null || purpose.equals(Purpose.EXPORT)) {
      formats.addAll(
          entityService.getPhenotypeExporterImplementations().stream()
              .map(
                  c -> {
                    Converter format =
                        new Converter().id(c.getSimpleName()).purpose(Purpose.EXPORT);
                    try {
                      PhenotypeExporter instance = c.getConstructor().newInstance();
                      format.setFileExtension(instance.getFileExtension());
                    } catch (Exception ignored) {
                    }
                    return format;
                  })
              .collect(Collectors.toList()));
    }

    formats.sort(Comparator.comparing(Converter::getId));
    return new ResponseEntity<>(formats, HttpStatus.OK);
  }

  @Override
  @Cacheable("expressionConstants")
  public ResponseEntity<List<Constant>> getExpressionConstants() {
    return new ResponseEntity<>(
        c2r.getConstants().stream()
            .map(ConstantEntity::getConstant)
            .sorted(Comparator.comparing(Constant::getTitle))
            .collect(Collectors.toList()),
        HttpStatus.OK);
  }
}
