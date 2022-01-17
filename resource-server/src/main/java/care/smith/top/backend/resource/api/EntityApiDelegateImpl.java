package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.EntityApiDelegate;
import care.smith.top.backend.model.Entity;
import care.smith.top.backend.resource.service.EntityService;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntityApiDelegateImpl implements EntityApiDelegate {
  @Autowired EntityService entityService;

  @Override
  public ResponseEntity<Entity> createEntity(
      String organisationName, String repositoryName, Entity entity, List<String> include) {
    return new ResponseEntity<>(
        entityService.createEntity(organisationName, repositoryName, entity), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Entity> getEntityById(
      String organisationName,
      String repositoryName,
      UUID id,
      Integer version,
      List<String> include) {
    return new ResponseEntity<>(
        entityService.loadEntity(organisationName, repositoryName, id, version), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteEntityById(
      String organisationName,
      String repositoryName,
      UUID id,
      Integer version,
      List<String> include,
      Boolean permanent) {
    entityService.deleteEntity(organisationName, repositoryName, id, version, permanent);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
