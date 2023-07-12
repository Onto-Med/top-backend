package care.smith.top.backend.api;

import static care.smith.top.backend.configuration.RequestValidator.isValidId;

import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Repository;
import care.smith.top.model.RepositoryPage;
import care.smith.top.model.RepositoryType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepositoryApiDelegateImpl implements RepositoryApiDelegate {
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;

  @Override
  public ResponseEntity<Repository> createRepository(
      String organisationId, Repository repository, List<String> include) {
    if (!isValidId(repository.getId()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The provided repository ID was invalid.");
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
  public ResponseEntity<RepositoryPage> getRepositories(
      List<String> include,
      String name,
      Boolean primary,
      RepositoryType repositoryType,
      Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toRepositoryPage(
            repositoryService.getRepositories(include, name, primary, repositoryType, page)));
  }

  @Override
  public ResponseEntity<RepositoryPage> getRepositoriesByOrganisationId(
      String organisationId,
      List<String> include,
      String name,
      RepositoryType repositoryType,
      Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toRepositoryPage(
            repositoryService.getRepositoriesByOrganisationId(
                organisationId, include, name, repositoryType, page)));
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

  @Override
  public ResponseEntity<Resource> exportRepository(
      String organisationId, String repositoryId, String converter) {
    ByteArrayOutputStream stream =
        entityService.exportRepository(organisationId, repositoryId, converter);
    return new ResponseEntity<>(new ByteArrayResource(stream.toByteArray()), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> importRepository(
      String organisationId, String repositoryId, String converter, MultipartFile file) {
    try {
      entityService.importRepository(
          organisationId, repositoryId, converter, file.getInputStream());
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not read uploaded file for repository import.",
          e);
    }
    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}
