package care.smith.top.backend.service;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.model.Repository;
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
  public Repository createRepository(String organisationId, Repository data, List<String> include) {
    if (repositoryRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Organisation '%s' does not exist!", organisationId)));

    // TODO: if (user is admin) ...
    RepositoryDao repository = new RepositoryDao(data).organisation(organisation);
    return repositoryRepository.save(repository).toApiModel();
  }

  @Transactional
  public void deleteRepository(String repositoryId, String organisationId, List<String> include) {
    repositoryRepository.delete(
        getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public List<Repository> getRepositories(
      List<String> include, String name, Boolean primary, Integer page) {
    // TODO: check if user has read permission
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    return repositoryRepository
        .findByNameAndPrimary(name, primary, pageRequest)
        .map(RepositoryDao::toApiModel)
        .getContent();
  }

  public List<Repository> getRepositoriesByOrganisationId(
      String organisationId, List<String> include, String name, Integer page) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    return repositoryRepository
        .findByOrganisationIdAndName(organisationId, name, pageRequest)
        .map(RepositoryDao::toApiModel)
        .getContent();
  }

  public Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    // TODO: include organisation if requested
    return getRepository(organisationId, repositoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .toApiModel();
  }

  public Optional<RepositoryDao> getRepository(String organisationId, String repositoryId) {
    return repositoryRepository
        .findById(repositoryId)
        .filter(r -> organisationId.equals(r.getOrganisation().getId()));
  }

  public boolean repositoryExists(String organisationId, String repositoryId) {
    return getRepository(organisationId, repositoryId).isPresent();
  }

  @Transactional
  public Repository updateRepository(
      String organisationId, String repositoryId, Repository data, List<String> include) {
    RepositoryDao repository =
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return repositoryRepository.saveAndFlush(repository.update(data)).toApiModel();
  }
}
