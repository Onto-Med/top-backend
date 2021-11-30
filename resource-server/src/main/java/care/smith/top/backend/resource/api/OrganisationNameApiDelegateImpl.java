package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.OrganisationNameApiDelegate;
import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.resource.service.EntityService;
import care.smith.top.backend.resource.service.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganisationNameApiDelegateImpl implements OrganisationNameApiDelegate {
  @Autowired OrganisationService organisationService;
  @Autowired EntityService entityService;

  @Override
  public ResponseEntity<Entity> createEntity(
      String organisationName, String repositoryName, Entity entity, List<String> include) {
    return new ResponseEntity<>(entity, HttpStatus.CREATED);
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
  public ResponseEntity<Entity> deleteEntityById(
      String organisationName,
      String repositoryName,
      UUID id,
      Integer version,
      List<String> include) {
    entityService.deleteEntity(organisationName, repositoryName, id, version);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Organisation> deleteOrganisationByName(
      String organisationName, List<String> include) {
    organisationService.deleteOrganisationByName(organisationName);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
