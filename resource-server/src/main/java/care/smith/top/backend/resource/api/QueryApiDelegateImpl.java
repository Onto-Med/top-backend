package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.QueryApiDelegate;
import care.smith.top.backend.model.Query;
import care.smith.top.backend.model.QueryResult;
import care.smith.top.backend.resource.service.PhenotypeQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QueryApiDelegateImpl implements QueryApiDelegate {
  @Autowired private PhenotypeQueryService phenotypeQueryService;

  @Override
  public ResponseEntity<Void> deleteQuery(
      String organisationId, String repositoryId, UUID queryId) {
    phenotypeQueryService.deleteQuery(organisationId, repositoryId, queryId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Query> enqueueQuery(
      String organisationId, String repositoryId, Query query) {
    phenotypeQueryService.enqueueQuery(organisationId, repositoryId, query);
    return new ResponseEntity<>(query, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<QueryResult> getQueryResult(
      String organisationId, String repositoryId, UUID queryId) {
    return new ResponseEntity<>(
        phenotypeQueryService.getQueryResult(organisationId, repositoryId, queryId), HttpStatus.OK);
  }
}
