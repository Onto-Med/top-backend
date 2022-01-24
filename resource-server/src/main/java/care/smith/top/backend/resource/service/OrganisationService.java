package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import care.smith.top.backend.resource.repository.DirectoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Service
public class OrganisationService {
  private final String directoryType = "organisation";
  @Autowired DirectoryRepository directoryRepository;

  public Organisation createOrganisation(Organisation organisation) {
    // TODO: use below code to get current user
    // UserDetails userDetails =
    //   (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //
    // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    // userDetails.getUsername());

    if (directoryRepository.findById(organisation.getOrganisationId().toString()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
    // TODO: update top-api to use strings as ids
    Directory directory = new Directory(organisation.getOrganisationId().toString());

    return directoryToOrganisation(directoryRepository.save(populate(directory, organisation)));
  }

  public Organisation updateOrganisationById(String id, Organisation organisation) {
    Directory directory =
        directoryRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return directoryToOrganisation(directoryRepository.save(populate(directory, organisation)));
  }

  public void deleteOrganisationByName(String organisationName) {
    Directory directory =
        directoryRepository
            .findById(organisationName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // TODO: handle subdirectories and content
    directoryRepository.delete(directory);
  }

  public Organisation getOrganisation(String organisationName, List<String> include) {
    return directoryToOrganisation(
        directoryRepository
            .findById(organisationName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
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
    organisation.setOrganisationId(Integer.parseInt(directory.getId()));
    organisation.setName(directory.getName());
    organisation.setDescription(directory.getDescription());
    organisation.setCreatedAt(directory.getCreatedAt().atOffset(ZoneOffset.UTC));
    // TODO: get super directories

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
    return directory
        .setName(organisation.getName())
        .setDescription(organisation.getDescription())
        .setTypes(Collections.singleton(directoryType));
    // TODO: set super directories
  }
}
