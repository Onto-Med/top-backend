package care.smith.top.backend.api;

import static care.smith.top.backend.configuration.RequestValidator.isValidId;

import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Organisation;
import care.smith.top.model.OrganisationMembership;
import care.smith.top.model.OrganisationPage;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
  public ResponseEntity<List<OrganisationMembership>> getOrganisationMemberships(
      String organisationId) {
    return ResponseEntity.ok(organisationService.getMemberships(organisationId));
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
  public ResponseEntity<OrganisationPage> getOrganisations(
      List<String> include, String name, Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toOrganisationPage(
            organisationService.getOrganisations(name, page, include)));
  }

  @Override
  public ResponseEntity<Void> deleteOrganisationById(String organisationId, List<String> include) {
    organisationService.deleteOrganisationById(organisationId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> createOrganisationMembership(
      String organisationId, OrganisationMembership organisationMembership) {
    organisationService.createOrganisationMembership(organisationId, organisationMembership);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteOrganisationMembership(
      String organisationId, OrganisationMembership organisationMembership) {
    organisationService.deleteOrganisationMembership(organisationId, organisationMembership);
    return ResponseEntity.noContent().build();
  }
}
