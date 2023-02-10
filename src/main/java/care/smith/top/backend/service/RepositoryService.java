package care.smith.top.backend.service;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.repository.OrganisationRepository;
import care.smith.top.backend.repository.RepositoryRepository;
import care.smith.top.model.Repository;
import care.smith.top.model.RepositoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Secured("ROLE_USER")
public class RepositoryService implements ContentService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private OrganisationRepository organisationRepository;

  @Override
  @Secured({})
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
  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  public void deleteRepository(String repositoryId, String organisationId, List<String> include) {
    repositoryRepository.delete(
        getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  @Secured({})
  public List<Repository> getRepositories(
      List<String> include,
      String name,
      Boolean primary,
      RepositoryType repositoryType,
      Integer page) {
    // TODO: check if user has read permission
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    return repositoryRepository
        .findByOrganisationIdAndNameAndPrimaryAndRepositoryType(
            null, name, primary, repositoryType, pageRequest)
        .map(RepositoryDao::toApiModel)
        .getContent();
  }

  @Secured({})
  public List<Repository> getRepositoriesByOrganisationId(
      String organisationId,
      List<String> include,
      String name,
      RepositoryType repositoryType,
      Integer page) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    return repositoryRepository
        .findByOrganisationIdAndNameAndPrimaryAndRepositoryType(
            organisationId, name, null, repositoryType, pageRequest)
        .map(RepositoryDao::toApiModel)
        .getContent();
  }

  @Secured({})
  public Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    // TODO: include organisation if requested
    return getRepository(organisationId, repositoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .toApiModel();
  }

  @Secured({})
  public Optional<RepositoryDao> getRepository(String organisationId, String repositoryId) {
    return repositoryRepository
        .findById(repositoryId)
        .filter(r -> organisationId.equals(r.getOrganisation().getId()));
  }

  @Secured({})
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
