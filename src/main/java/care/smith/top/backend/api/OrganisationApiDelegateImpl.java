package care.smith.top.backend.api;

import care.smith.top.model.Organisation;
import care.smith.top.backend.service.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static care.smith.top.backend.configuration.RequestValidator.isValidId;

@Service
public class OrganisationApiDelegateImpl implements OrganisationApiDelegate {
  @Autowired OrganisationService organisationService;

  @Override
  public ResponseEntity<Organisation> createOrganisation(
      Organisation organisation, List<String> include) {
    if (!isValidId(organisation.getId()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The provided organisation ID was invalid.");
    return new ResponseEntity<>(
        organisationService.createOrganisation(organisation), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Organisation> updateOrganisationById(
      String organisationId, Organisation organisation, List<String> include) {
    return new ResponseEntity<>(
        organisationService.updateOrganisationById(organisationId, organisation), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Organisation> getOrganisationById(
      String organisationId, List<String> include) {
    return new ResponseEntity<>(
        organisationService.getOrganisation(organisationId, include), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<Organisation>> getOrganisations(
      List<String> include, String name, Integer page) {
    return new ResponseEntity<>(
        organisationService.getOrganisations(name, page, include), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteOrganisationById(String organisationId, List<String> include) {
    organisationService.deleteOrganisationById(organisationId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
