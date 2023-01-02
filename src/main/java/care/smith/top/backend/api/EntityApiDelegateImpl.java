package care.smith.top.backend.api;

import care.smith.top.model.DataType;
import care.smith.top.model.Entity;
import care.smith.top.model.EntityType;
import care.smith.top.backend.service.EntityService;
import care.smith.top.model.ItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringWriter;
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
  public ResponseEntity<String> exportEntity(
      String organisationId, String repositoryId, String id, String format, Integer version) {
    StringWriter writer =
        entityService.exportEntity(organisationId, repositoryId, id, format, version);
    return new ResponseEntity<>(writer.toString(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteEntityById(
      String organisationId,
      String repositoryId,
      String id,
      Integer version,
      List<String> include) {
    if (version != null) {
      entityService.deleteVersion(organisationId, repositoryId, id, version);
    } else {
      entityService.deleteEntity(organisationId, repositoryId, id);
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
  public ResponseEntity<List<Entity>> getEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      Integer page) {
    return new ResponseEntity<>(
        entityService.getEntitiesByRepositoryId(
            organisationId, repositoryId, include, name, type, dataType, itemType, page),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<Entity>> getEntities(
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      Integer page) {
    return new ResponseEntity<>(
        entityService.getEntities(include, name, type, dataType, itemType, page), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<Entity>> getRootEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      Integer page) {
    // TODO: result is currently unpaged and 'page' parameter is ignored
    return new ResponseEntity<>(
        entityService.getRootEntitiesByRepositoryId(
            organisationId, repositoryId, include, name, type, dataType, itemType),
        HttpStatus.OK);
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
