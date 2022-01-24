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

@Service
public class OrganisationService {
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
    directory.setName(organisation.getName());
    directory.setDescription(organisation.getDescription());
    directory.setTypes(Collections.singleton("organisation"));
    // TODO: set super directories
    directory = directoryRepository.save(directory);

    Organisation result = new Organisation();
    result.setOrganisationId(Integer.parseInt(directory.getId()));
    result.setName(directory.getName());
    result.setDescription(directory.getDescription());
    result.setCreatedAt(directory.getCreatedAt().atOffset(ZoneOffset.UTC));
    // TODO: get super directories

    return result;
  }

  public void deleteOrganisationByName(String organisationName) {
    Directory directory =
        directoryRepository
            .findById(organisationName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // TODO: handle subdirectories and content
    directoryRepository.delete(directory);
  }
}
