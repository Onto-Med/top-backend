package care.smith.top.backend.api;

import care.smith.top.backend.service.PhenotypeQueryService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Query;
import care.smith.top.model.QueryPage;
import care.smith.top.model.QueryResult;
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
  public ResponseEntity<QueryResult> enqueueQuery(
      String organisationId, String repositoryId, Query query) {
    return new ResponseEntity<>(
        phenotypeQueryService.enqueueQuery(organisationId, repositoryId, query),
        HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<QueryPage> getQueries(
      String organisationId, String repositoryId, Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toQueryPage(
            phenotypeQueryService.getQueries(organisationId, repositoryId, page)));
  }

  @Override
  public ResponseEntity<QueryResult> getQueryResult(
      String organisationId, String repositoryId, UUID queryId) {
    return new ResponseEntity<>(
        phenotypeQueryService.getQueryResult(organisationId, repositoryId, queryId), HttpStatus.OK);
  }
}
