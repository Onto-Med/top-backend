package care.smith.top.backend.service;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.repository.OrganisationRepository;
import care.smith.top.model.Organisation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrganisationService implements ContentService {
  @Autowired OrganisationRepository organisationRepository;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  @Override
  public long count() {
    return organisationRepository.count();
  }

  @Transactional
  public Organisation createOrganisation(Organisation data) {
    // TODO: use below code to get current user
    // UserDetails userDetails =
    //   (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //
    // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    // userDetails.getUsername());

    if (organisationRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    OrganisationDao organisation = new OrganisationDao(data);
    if (data.getSuperOrganisation() != null)
      organisationRepository
          .findById(data.getSuperOrganisation().getId())
          .ifPresent(organisation::superOrganisation);

    return organisationRepository.save(organisation).toApiModel();
  }

  @Transactional
  public Organisation updateOrganisationById(String id, Organisation data) {
    OrganisationDao organisation =
        organisationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (data.getSuperOrganisation() != null)
      organisationRepository
          .findById(data.getSuperOrganisation().getId())
          .ifPresent(organisation::superOrganisation);

    return organisationRepository.saveAndFlush(organisation.update(data)).toApiModel();
  }

  @Transactional
  public void deleteOrganisationById(String organisationId) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    for (OrganisationDao subOrganisation : organisation.getSubOrganisations()) {
      subOrganisation.superOrganisation(null);
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
