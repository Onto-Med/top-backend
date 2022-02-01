package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.RepositoryApiDelegate;
import care.smith.top.backend.model.Repository;
import care.smith.top.backend.resource.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepositoryApiDelegateImpl implements RepositoryApiDelegate {
  @Autowired RepositoryService repositoryService;

  @Override
  public ResponseEntity<Repository> createRepository(
      String organisationId, Repository repository, List<String> include) {
    return new ResponseEntity<>(
        repositoryService.createRepository(organisationId, repository, include),
        HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteRepositoryById(
      String repositoryId, String organisationId, List<String> include) {
    repositoryService.deleteRepository(repositoryId, organisationId, include);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<List<Repository>> getRepositories(
      List<String> include, String name, Integer page) {
    return RepositoryApiDelegate.super.getRepositories(include, name, page);
  }

  @Override
  public ResponseEntity<List<Repository>> getRepositoriesByOrganisationId(
      String organisationId, List<String> include, String name, Integer page) {
    return RepositoryApiDelegate.super.getRepositoriesByOrganisationId(
        organisationId, include, name, page);
  }

  @Override
  public ResponseEntity<Repository> getRepositoryById(
      String organisationId, String repositoryId, List<String> include) {
    return new ResponseEntity<>(
        repositoryService.getRepository(organisationId, repositoryId, include), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Repository> updateRepositoryById(
      String organisationId, String repositoryId, Repository repository, List<String> include) {
    return new ResponseEntity<>(
        repositoryService.updateRepository(organisationId, repositoryId, repository, include),
        HttpStatus.OK);
  }
}
