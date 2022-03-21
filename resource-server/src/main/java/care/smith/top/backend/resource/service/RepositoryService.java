package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import care.smith.top.backend.neo4j_ontology_access.repository.DirectoryRepository;
import care.smith.top.backend.neo4j_ontology_access.repository.RepositoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepositoryService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private DirectoryRepository directoryRepository;

  @Transactional
  public care.smith.top.backend.model.Repository createRepository(
      String organisationId, care.smith.top.backend.model.Repository data, List<String> include) {
    Directory organisation = getOrganisation(organisationId);

    if (repositoryRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    // TODO: let users set 'primary' field of repositories
    Repository repository =
        (Repository)
            new Repository(data.getId())
                .setName(data.getName())
                .setDescription(data.getDescription())
                .addSuperDirectory(organisation); // TODO: super directory not stored in db!

    return repositoryToApiPojo(repositoryRepository.save(repository));
  }

  public void deleteRepository(String repositoryId, String organisationId, List<String> include) {
    repositoryRepository.delete(
        repositoryRepository
            .findByIdAndSuperDirectoryId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public care.smith.top.backend.model.Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    // TODO: include organisation if requested
    return repositoryToApiPojo(
        repositoryRepository
            .findByIdAndSuperDirectoryId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public care.smith.top.backend.model.Repository updateRepository(
      String organisationId,
      String repositoryId,
      care.smith.top.backend.model.Repository data,
      List<String> include) {
    Repository repository =
        repositoryRepository
            .findByIdAndSuperDirectoryId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    repository.setName(data.getName()).setDescription(data.getDescription());
    return repositoryToApiPojo(repositoryRepository.save(repository));
  }

  public List<care.smith.top.backend.model.Repository> getRepositories(
      List<String> include, String name, Integer page) {
    // TODO: check if user has read permission
    int requestedPage = page == null ? 0 : page - 1;
    return repositoryRepository
        .findByNameContainingIgnoreCase(
            name, PageRequest.of(requestedPage, pageSize, Sort.by("r.name")))
        .stream()
        .map(this::repositoryToApiPojo)
        .collect(Collectors.toList());
  }

  public List<care.smith.top.backend.model.Repository> getRepositoriesByOrganisationId(
      String organisationId, List<String> include, String name, Integer page) {
    int requestedPage = page == null ? 0 : page - 1;
    return repositoryRepository
        .findByNameContainingAndSuperDirectoryId(
            name, organisationId, PageRequest.of(requestedPage, pageSize, Sort.by("r.name")))
        .stream()
        .map(this::repositoryToApiPojo)
        .collect(Collectors.toList());
  }

  /**
   * Convert {@link Repository} object to {@link care.smith.top.backend.model.Repository} object.
   *
   * @param repository The object to be converted.
   * @return The resulting {@link care.smith.top.backend.model.Repository} object.
   */
  private care.smith.top.backend.model.Repository repositoryToApiPojo(Repository repository) {
    care.smith.top.backend.model.Repository data =
        new care.smith.top.backend.model.Repository()
            .createdAt(repository.getCreatedAtOffset())
            .id(repository.getId())
            .name(repository.getName())
            .description(repository.getDescription());

    repository.getSuperDirectories().stream()
        .findFirst()
        .ifPresent(o -> data.setOrganisation(new Organisation().name(o.getName()).id(o.getId())));

    return data;
  }

  private Directory getOrganisation(String organisationId) {
    return directoryRepository
        .findById(organisationId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Organisation '%s' does not exist!", organisationId)));
  }
}
