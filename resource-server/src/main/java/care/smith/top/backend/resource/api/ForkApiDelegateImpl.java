package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.ForkApiDelegate;
import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.ForkCreateInstruction;
import care.smith.top.backend.model.ForkUpdateInstruction;
import care.smith.top.backend.resource.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForkApiDelegateImpl implements ForkApiDelegate {
  @Autowired EntityService entityService;

  @Override
  public ResponseEntity<List<Entity>> createFork(
      String organisationId,
      String repositoryId,
      String id,
      ForkCreateInstruction forkCreateInstruction,
      List<String> include,
      Integer version) {
    return new ResponseEntity<>(
        entityService.createFork(
            organisationId, repositoryId, id, forkCreateInstruction, version, include),
        HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<List<Entity>> getForks(
      String organisationId, String repositoryId, String id, List<String> include) {
    return new ResponseEntity<>(
        entityService.getForks(organisationId, repositoryId, id, include), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<Entity>> updateFork(
      String organisationId,
      String repositoryId,
      String id,
      ForkUpdateInstruction forkUpdateInstruction,
      List<String> include) {
    return ForkApiDelegate.super.updateFork(
        organisationId, repositoryId, id, forkUpdateInstruction, include);
  }
}
