package care.smith.top.backend.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.repository.OrganisationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrganisationService implements ContentService {
  private final String directoryType = "Organisation";
  @Autowired OrganisationRepository organisationRepository;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  @Override
  public long count() {
    return organisationRepository.count();
  }

  @Transactional
  public Organisation createOrganisation(Organisation organisation) {
    // TODO: use below code to get current user
    // UserDetails userDetails =
    //   (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //
    // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    // userDetails.getUsername());

    if (organisation.getId() == null) organisation.id(UUID.randomUUID().toString());

    if (organisationRepository.findById(organisation.getId()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    return organisationRepository.save(organisation);
  }

  @Transactional
  public Organisation updateOrganisationById(String id, Organisation data) {
    Organisation organisation =
        organisationRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    organisation.setSuperOrganisation(data.getSuperOrganisation());
    organisation.setDescription(data.getDescription());
    organisation.setName(data.getName());

    return organisationRepository.save(organisation);
  }

  @Transactional
  public void deleteOrganisationById(String organisationId) {
    Organisation organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    for (Organisation subOrganisation : organisation.getSubOrganisations()) {
      subOrganisation.setSuperOrganisation(null);
      organisationRepository.save(subOrganisation);
    }
    organisationRepository.delete(organisation);
  }

  public Organisation getOrganisation(String organisationId, List<String> include) {
    return organisationRepository
        .findById(organisationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public List<Organisation> getOrganisations(String name, Integer page, List<String> include) {
    PageRequest pageRequest = PageRequest.of(page == null ? 1 : page - 1, pageSize);
    if (name == null) return organisationRepository.findAll(pageRequest).getContent();
    return organisationRepository
        .findAllByNameIsContainingIgnoreCaseOrDescriptionIsContainingIgnoreCase(
            name, name, pageRequest)
        .getContent();
  }
}
