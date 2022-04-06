package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import care.smith.top.backend.neo4j_ontology_access.repository.DirectoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrganisationService implements ContentService {
  private final String directoryType = "Organisation";
  @Autowired DirectoryRepository directoryRepository;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  @Override
  public long count() {
    return directoryRepository.count();
  }

  public Organisation createOrganisation(Organisation organisation) {
    // TODO: use below code to get current user
    // UserDetails userDetails =
    //   (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //
    // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    // userDetails.getUsername());

    if (organisation.getId() == null) organisation.id(UUID.randomUUID().toString());

    if (directoryRepository.findById(organisation.getId()).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    Directory directory = new Directory(organisation.getId());

    return directoryToOrganisation(directoryRepository.save(populate(directory, organisation)));
  }

  public Organisation updateOrganisationById(String id, Organisation organisation) {
    Directory directory =
        directoryRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return directoryToOrganisation(directoryRepository.save(populate(directory, organisation)));
  }

  public void deleteOrganisationById(String organisationId) {
    Directory directory =
        directoryRepository
            .findById(organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // TODO: handle subdirectories and content
    directoryRepository.delete(directory);
  }

  public Organisation getOrganisation(String organisationId, List<String> include) {
    return directoryToOrganisation(
        directoryRepository
            .findById(organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public List<Organisation> getOrganisations(String name, Integer page, List<String> include) {
    return directoryRepository.findAllByTypeAndNameAndDescription("Organisation", name, null).stream()
        .map(this::directoryToOrganisation)
        .collect(Collectors.toList());
  }

  /**
   * Transform a {@link Directory} object with type {@link OrganisationService#directoryType} to an
   * {@link Organisation} object.
   *
   * @param directory The directory to be transformed.
   * @return The resulting organisation object.
   */
  private Organisation directoryToOrganisation(Directory directory) {
    if (!directory.getTypes().contains(directoryType))
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          String.format("Directory is not of type %s", directoryType));

    Organisation organisation = new Organisation();
    organisation.setId(directory.getId());
    organisation.setName(directory.getName());
    organisation.setDescription(directory.getDescription());
    organisation.setCreatedAt(directory.getCreatedAtOffset());
    if (directory.getSuperDirectories() != null)
      directory.getSuperDirectories().stream()
          .findFirst()
          .ifPresent(value -> organisation.setSuperOrganisation(directoryToOrganisation(value)));

    return organisation;
  }

  /**
   * Populates the given {@link Directory} object with values from the given {@link Organisation}
   * object.
   *
   * @param directory The directory to be populated with data.
   * @param organisation The organisation providing data.
   * @return The modified directory.
   */
  private Directory populate(Directory directory, Organisation organisation) {
    if (organisation.getSuperOrganisation() != null
        && organisation.getSuperOrganisation().getId() != null) {
      String superOrganisationId = organisation.getSuperOrganisation().getId();
      directory.setSuperDirectories(
          Collections.singleton(
              directoryRepository
                  .findById(superOrganisationId)
                  .orElseThrow(
                      () ->
                          new ResponseStatusException(
                              HttpStatus.CONFLICT,
                              String.format(
                                  "Super organisation %s does not exist.", superOrganisationId)))));
    } else {
      directory.setSuperDirectories(null);
    }

    return directory
        .setName(organisation.getName())
        .setDescription(organisation.getDescription())
        .setTypes(Collections.singleton(directoryType));
  }
}
