package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.RepositoryApiDelegate;
import care.smith.top.backend.model.Repository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepositoryApiDelegateImpl implements RepositoryApiDelegate {
  @Override
  public ResponseEntity<Repository> createRepository(String organisationId, Repository repository, List<String> include) {
    return RepositoryApiDelegate.super.createRepository(organisationId, repository, include);
  }

  @Override
  public ResponseEntity<Void> deleteRepositoryById(String repositoryId, String organisationId, List<String> include) {
    return RepositoryApiDelegate.super.deleteRepositoryById(repositoryId, organisationId, include);
  }

  @Override
  public ResponseEntity<List<Repository>> getRepositories(List<String> include, String name, Integer page) {
    return RepositoryApiDelegate.super.getRepositories(include, name, page);
  }

  @Override
  public ResponseEntity<List<Repository>> getRepositoriesByOrganisationId(String organisationId, List<String> include, String name, Integer page) {
    return RepositoryApiDelegate.super.getRepositoriesByOrganisationId(organisationId, include, name, page);
  }

  @Override
  public ResponseEntity<Repository> getRepositoryById(String organisationId, String repositoryId, List<String> include) {
    return RepositoryApiDelegate.super.getRepositoryById(organisationId, repositoryId, include);
  }

  @Override
  public ResponseEntity<Repository> updateRepositoryById(String organisationId, String repositoryId, Repository repository, List<String> include) {
    return RepositoryApiDelegate.super.updateRepositoryById(organisationId, repositoryId, repository, include);
  }
}
