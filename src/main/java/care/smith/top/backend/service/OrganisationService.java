package care.smith.top.backend.service;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.repository.OrganisationRepository;
import care.smith.top.model.Organisation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * This service provides organisation related business logic. If oauth2 authentication is enabled
 * the following restrictions will be applied to CRUD operations:
 *
 * <ul>
 *   <li>Create: authenticated users
 *   <li>Read: anonymous users
 *   <li>Update: authenticated users with write authority
 *   <li>Delete: authenticated users with write authority
 * </ul>
 */
@Service
public class OrganisationService implements ContentService {
  @Autowired OrganisationRepository organisationRepository;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  @Override
  public long count() {
    return organisationRepository.count();
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional
  public Organisation createOrganisation(Organisation data) {
    if (organisationRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    OrganisationDao organisation = new OrganisationDao(data);
    if (data.getSuperOrganisation() != null)
      organisationRepository
          .findById(data.getSuperOrganisation().getId())
          .ifPresent(organisation::superOrganisation);

    return organisationRepository.save(organisation).toApiModel();
  }

  @PreAuthorize("isAuthenticated() and hasAuthority('write')")
  @Transactional
  public Organisation updateOrganisationById(String id, Organisation data) {
    OrganisationDao organisation =
        organisationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (data.getSuperOrganisation() != null && !id.equals(data.getSuperOrganisation().getId()))
      organisationRepository
          .findById(data.getSuperOrganisation().getId())
          .ifPresent(organisation::superOrganisation);

    return organisationRepository.saveAndFlush(organisation.update(data)).toApiModel();
  }

  @PreAuthorize("isAuthenticated() and hasAuthority('manage')")
  @Transactional
  public void deleteOrganisationById(String organisationId) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    for (OrganisationDao subOrganisation : organisation.getSubOrganisations()) {
      subOrganisation.superOrganisation(organisation.getSuperOrganisation());
      organisationRepository.save(subOrganisation);
    }
    organisationRepository.delete(organisation);
  }

  public Organisation getOrganisation(String organisationId, List<String> include) {
    return organisationRepository
        .findById(organisationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .toApiModel();
  }

  public List<Organisation> getOrganisations(String name, Integer page, List<String> include) {
    PageRequest pageRequest = PageRequest.of(page == null ? 1 : page - 1, pageSize);
    return organisationRepository
        .findAllByNameOrDescription(name, pageRequest)
        .map(OrganisationDao::toApiModel)
        .getContent();
  }
}
