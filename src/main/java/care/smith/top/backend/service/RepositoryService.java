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
import java.util.stream.Collectors;

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
    repository.setOrganisation(organisation);
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
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    if (name == null) {
      if (primary == null) return repositoryRepository.findAll(pageRequest).getContent();
      return repositoryRepository.findAllByPrimary(primary, pageRequest).getContent();
    }
    if (primary == null)
      return repositoryRepository.findByNameContainingIgnoreCase(name, pageRequest).getContent();
    return repositoryRepository
        .findByNameContainingIgnoreCaseAndPrimary(name, primary, pageRequest)
        .getContent();
  }

  public List<care.smith.top.backend.model.Repository> getRepositoriesByOrganisationId(
      String organisationId, List<String> include, String name, Integer page) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    if (name == null)
      return repositoryRepository.findByOrganisationId(organisationId, pageRequest).getContent();
    return repositoryRepository
        .findByOrganisationIdAndNameContainingIgnoreCase(organisationId, name, pageRequest)
        .getContent();
  }

  public care.smith.top.backend.model.Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    // TODO: include organisation if requested
    return getRepository(organisationId, repositoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public Optional<Repository> getRepository(String organisationId, String repositoryId) {
    return repositoryRepository
        .findById(repositoryId)
        .filter(r -> organisationId.equals(r.getOrganisation().getId()));
  }

  public boolean repositoryExists(String organisationId, String repositoryId) {
    return getRepository(organisationId, repositoryId).isPresent();
  }

  @Transactional
  public care.smith.top.backend.model.Repository updateRepository(
      String organisationId, String repositoryId, Repository data, List<String> include) {
    Repository repository =
        repositoryRepository
            .findById(repositoryId)
            .filter(r -> organisationId.equals(r.getOrganisation().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    repository.setDescription(data.getDescription());
    repository.setName(data.getName());
    // TODO: if (user is admin) ...
    repository.setPrimary(data.isPrimary());
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
