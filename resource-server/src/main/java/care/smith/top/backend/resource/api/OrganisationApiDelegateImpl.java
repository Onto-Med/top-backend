package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.OrganisationApiDelegate;
import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.resource.service.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganisationApiDelegateImpl implements OrganisationApiDelegate {
  @Autowired OrganisationService organisationService;

  @Override
  public ResponseEntity<Organisation> createOrganisation(
      Organisation organisation, List<String> include) {
    return new ResponseEntity<>(
        organisationService.createOrganisation(organisation), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Organisation> updateOrganisationByName(String organisationName, Organisation organisation, List<String> include) {
    return new ResponseEntity<>(organisationService.updateOrganisationById(organisationName, organisation), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteOrganisationByName(
      String organisationName, List<String> include) {
    organisationService.deleteOrganisationByName(organisationName);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
