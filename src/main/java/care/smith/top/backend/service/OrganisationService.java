package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.repository.jpa.OrganisationMembershipRepository;
import care.smith.top.backend.repository.jpa.OrganisationRepository;
import care.smith.top.model.DataSource;
import care.smith.top.model.Organisation;
import care.smith.top.model.OrganisationMembership;
import care.smith.top.model.QueryType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
  private static final Logger LOGGER = Logger.getLogger(OrganisationService.class.getName());

  @Autowired OrganisationRepository organisationRepository;
  @Autowired UserService userService;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  @Value("${top.phenotyping.result.dir:config/query_results}")
  private String resultDir;

  @Autowired private OrganisationMembershipRepository organisationMembershipRepository;

  @Override
  public long count() {
    return organisationRepository.count();
  }

  @PreAuthorize("hasRole('USER')")
  @Transactional
  public Organisation createOrganisation(Organisation data) {
    if (organisationRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    OrganisationDao organisation = new OrganisationDao(data);
    if (data.getSuperOrganisation() != null)
      organisationRepository
          .findById(data.getSuperOrganisation().getId())
          .ifPresent(organisation::superOrganisation);

    UserDao user = userService.getCurrentUser();
    if (user != null) organisation.setMemberPermission(user, Permission.MANAGE);

    return organisationRepository.save(organisation).toApiModel(user);
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  @Transactional
  public Organisation updateOrganisationById(String organisationId, Organisation data) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (data.getSuperOrganisation() != null
        && !organisationId.equals(data.getSuperOrganisation().getId()))
      organisationRepository
          .findById(data.getSuperOrganisation().getId())
          .ifPresent(organisation::superOrganisation);

    return organisationRepository
        .saveAndFlush(organisation.update(data))
        .toApiModel(userService.getCurrentUser());
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'MANAGE')")
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

    Path organisationPath = Paths.get(resultDir, organisationId);
    if (!organisationPath.startsWith(Paths.get(resultDir)))
      LOGGER.severe(
          String.format(
              "Organisation directory '%s' is invalid and cannot be deleted!", organisationPath));
    try {
      Files.deleteIfExists(organisationPath);
    } catch (IOException e) {
      LOGGER.severe(
          String.format(
              "Could not delete organisation directory '%s'! Cause: %s",
              organisationPath, e.getMessage()));
    }
  }

  @Transactional
  public Organisation getOrganisation(String organisationId, List<String> include) {
    return organisationRepository
        .findById(organisationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .toApiModel(userService.getCurrentUser());
  }

  @Transactional
  public Page<Organisation> getOrganisations(String name, Integer page, List<String> include) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 1 : page - 1, pageSize, Sort.by(OrganisationDao_.NAME));
    return organisationRepository
        .findAllByNameOrDescription(name, pageRequest)
        .map(o -> o.toApiModel(userService.getCurrentUser()));
  }

  @PreAuthorize("hasRole('USER')")
  @Transactional
  public List<OrganisationMembership> getMemberships(String organisationId) {
    return organisationMembershipRepository.findAllByOrganisation_Id(organisationId).stream()
        .map(OrganisationMembershipDao::toApiModel)
        .collect(Collectors.toList());
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'MANAGE')")
  @Transactional
  public void createOrganisationMembership(
      String organisationId, OrganisationMembership organisationMembership) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organisation does not exist."));
    UserDao user =
        userService
            .getUserById(organisationMembership.getUser().getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist."));
    userService.grantMembership(
        organisation, user, Permission.fromApiModel(organisationMembership.getPermission()));
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'MANAGE')")
  @Transactional
  public void deleteOrganisationMembership(
      String organisationId, OrganisationMembership organisationMembership) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organisation does not exist."));
    UserDao user =
        userService
            .getUserById(organisationMembership.getUser().getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist."));
    userService.revokeMembership(organisation, user);
  }

  @Transactional
  public Collection<String> getOrganisationDataSourceIds(
      String organisationId, QueryType queryType) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organisation does not exist."));

    return organisation.getDataSourcesByQueryType(queryType).stream()
        .map(OrganisationDataSourceDao::getDataSourceId)
        .collect(Collectors.toSet());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void addOrganisationDataSource(String organisationId, DataSource dataSource) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organisation does not exist."));

    OrganisationDataSourceDao dataSourceDao =
        new OrganisationDataSourceDao(organisation, dataSource.getId(), dataSource.getQueryType());
    organisationRepository.save(organisation.addDataSource(dataSourceDao));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void removeOrganisationDataSource(String organisationId, DataSource dataSource) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organisation does not exist."));

    if (!organisation.removeDataSource(dataSource.getId(), dataSource.getQueryType()))
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Organisation has no such data source.");

    organisationRepository.save(organisation);
  }
}
