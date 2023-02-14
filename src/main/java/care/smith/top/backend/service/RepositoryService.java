package care.smith.top.backend.service;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.model.Role;
import care.smith.top.backend.model.UserDao;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
  @Autowired private UserService userService;

  @Override
  public long count() {
    return repositoryRepository.count();
  }

  @Transactional
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
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

    UserDao user = userService.getCurrentUser();
    if (user == null || !user.getRole().equals(Role.ADMIN)) {
      data.setPrimary(false);
    }

    RepositoryDao repository = new RepositoryDao(data).organisation(organisation);
    return repositoryRepository.save(repository).toApiModel();
  }

  @Transactional
  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public void deleteRepository(String repositoryId, String organisationId, List<String> include) {
    repositoryRepository.delete(
        getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  // TODO: restrict access to organisations with read permission and primary repositories
  public List<Repository> getRepositories(
      List<String> include,
      String name,
      Boolean primary,
      RepositoryType repositoryType,
      Integer page) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by("name"));
    return repositoryRepository
        .findByOrganisationIdAndNameAndPrimaryAndRepositoryType(
            null, name, primary, repositoryType, pageRequest)
        .map(RepositoryDao::toApiModel)
        .getContent();
  }

  // TODO: allow read access to primary repositories
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'READ')")
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

  // TODO: allow read access to primary repositories
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'READ')")
  public Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    return getRepository(organisationId, repositoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .toApiModel();
  }

  // TODO: make this method private
  public Optional<RepositoryDao> getRepository(String organisationId, String repositoryId) {
    return repositoryRepository
        .findById(repositoryId)
        .filter(r -> organisationId.equals(r.getOrganisation().getId()));
  }

  // TODO: make this method private
  public boolean repositoryExists(String organisationId, String repositoryId) {
    return getRepository(organisationId, repositoryId).isPresent();
  }

  @Transactional
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public Repository updateRepository(
      String organisationId, String repositoryId, Repository data, List<String> include) {
    RepositoryDao repository =
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return repositoryRepository.saveAndFlush(repository.update(data)).toApiModel();
  }
}
