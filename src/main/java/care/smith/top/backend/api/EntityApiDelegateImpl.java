package care.smith.top.backend.api;

import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static care.smith.top.backend.configuration.RequestValidator.isValidId;

@Service
public class EntityApiDelegateImpl implements EntityApiDelegate {
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
}
