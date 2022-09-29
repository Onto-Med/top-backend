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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RepositoryService implements ContentService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private DirectoryRepository directoryRepository;

  @Override
  public long count() {
    return repositoryRepository.count();
  }

  @Transactional
  public care.smith.top.backend.model.Repository createRepository(
      String organisationId, care.smith.top.backend.model.Repository data, List<String> include) {
    Directory organisation = getOrganisation(organisationId);

    if (repositoryRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    Repository repository =
        (Repository)
            new Repository(data.getId())
                .setName(data.getName())
                .setDescription(data.getDescription())
                .addSuperDirectory(organisation);

    // TODO: if (user is admin) ...
    if (data.isPrimary() != null) repository.setPrimary(data.isPrimary());

    return repositoryToApiPojo(repositoryRepository.save(repository));
  }

  @Transactional
  public void deleteRepository(String repositoryId, String organisationId, List<String> include) {
    repositoryRepository.delete(
        getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public List<care.smith.top.backend.model.Repository> getRepositories(
      List<String> include, String name, Boolean primary, Integer page) {
    // TODO: check if user has read permission
    // TODO: filter by `primary` property
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

  public care.smith.top.backend.model.Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    // TODO: include organisation if requested
    Repository repository =
        getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return repositoryToApiPojo(repository);
  }

  public Optional<Repository> getRepository(String organisationId, String repositoryId) {
    return repositoryRepository.findByIdAndSuperDirectoryId(repositoryId, organisationId);
  }

  public boolean repositoryExists(String organisationId, String repositoryId) {
    return getRepository(organisationId, repositoryId).isPresent();
  }

  public care.smith.top.backend.model.Repository updateRepository(
      String organisationId,
      String repositoryId,
      care.smith.top.backend.model.Repository data,
      List<String> include) {
    Repository repository =
        getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    // TODO: if (user is admin) ...
    // if (data.isPrimary() != null) repository.setPrimary(data.isPrimary());

    repository.setName(data.getName()).setDescription(data.getDescription());
    return repositoryToApiPojo(repositoryRepository.save(repository));
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
            .primary(repository.isPrimary())
            .name(repository.getName())
            .description(repository.getDescription());

    repository.getSuperDirectories().stream()
        .findFirst()
        .ifPresent(o -> data.setOrganisation(new Organisation().name(o.getName()).id(o.getId())));

    return data;
  }
}
