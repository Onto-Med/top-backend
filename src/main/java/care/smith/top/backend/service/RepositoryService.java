package care.smith.top.backend.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.model.Repository;
import care.smith.top.backend.repository.OrganisationRepository;
import care.smith.top.backend.repository.RepositoryRepository;
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

@Service
public class RepositoryService implements ContentService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private OrganisationRepository organisationRepository;

  @Override
  public long count() {
    return repositoryRepository.count();
  }

  @Transactional
  public Repository createRepository(
      String organisationId, Repository repository, List<String> include) {
    Organisation organisation = getOrganisation(organisationId);

    if (repositoryRepository.existsById(repository.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    // TODO: if (user is admin) ...
    return repositoryRepository.save(repository);
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
        .getContent();
  }

  public List<care.smith.top.backend.model.Repository> getRepositoriesByOrganisationId(
      String organisationId, List<String> include, String name, Integer page) {
    int requestedPage = page == null ? 0 : page - 1;
    return repositoryRepository
        .findByNameContainingAndOrganisationId(
            name, organisationId, PageRequest.of(requestedPage, pageSize, Sort.by("r.name")))
        .getContent();
  }

  public care.smith.top.backend.model.Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    // TODO: include organisation if requested
    return getRepository(organisationId, repositoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public Optional<Repository> getRepository(String organisationId, String repositoryId) {
    return repositoryRepository.findByIdAndOrganisationId(repositoryId, organisationId);
  }

  public boolean repositoryExists(String organisationId, String repositoryId) {
    return getRepository(organisationId, repositoryId).isPresent();
  }

  public care.smith.top.backend.model.Repository updateRepository(
      String organisationId, String repositoryId, Repository repository, List<String> include) {
    if (!repositoryExists(organisationId, repositoryId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    // TODO: if (user is admin) ...
    return repositoryRepository.save(repository);
  }

  private Organisation getOrganisation(String organisationId) {
    return organisationRepository
        .findById(organisationId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Organisation '%s' does not exist!", organisationId)));
  }
}
