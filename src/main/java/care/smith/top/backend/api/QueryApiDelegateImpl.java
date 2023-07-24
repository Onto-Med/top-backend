package care.smith.top.backend.api;

import care.smith.top.backend.repository.jpa.QueryRepository;
import care.smith.top.backend.service.PhenotypeQueryService;
import care.smith.top.backend.service.QueryService;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import java.io.File;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QueryApiDelegateImpl implements QueryApiDelegate {
  @Autowired private PhenotypeQueryService phenotypeQueryService;
  @Autowired private DocumentQueryService documentQueryService;
  @Autowired private QueryRepository queryRepository;

  @Override
  public ResponseEntity<Void> deleteQuery(
      String organisationId, String repositoryId, UUID queryId) {
    getQueryService(organisationId, repositoryId, queryId)
        .deleteQuery(organisationId, repositoryId, queryId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Resource> downloadQueryResult(
      String organisationId, String repositoryId, UUID queryId) {
    try {
      File file =
          phenotypeQueryService.getQueryResultPath(organisationId, repositoryId, queryId).toFile();
      ContentDisposition contentDisposition =
          ContentDisposition.builder("inline").filename(file.getName()).build();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentDisposition(contentDisposition);
      return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    } catch (FileSystemException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Query result is not accessible.", e);
    }
  }

  @Override
  public ResponseEntity<QueryResult> enqueueQuery(
      String organisationId, String repositoryId, Query query) {
    switch (query.getType()) {
      case PHENOTYPE:
        return new ResponseEntity<>(
            phenotypeQueryService.enqueueQuery(organisationId, repositoryId, query),
            HttpStatus.CREATED);
      case CONCEPT:
        return new ResponseEntity<>(
            documentQueryService.enqueueQuery(organisationId, repositoryId, query),
            HttpStatus.CREATED);
      default:
        throw new ResponseStatusException(
            HttpStatus.NOT_ACCEPTABLE, "Query type is neither Phenotype nor Concept.");
    }
  }

  @Override
  public ResponseEntity<List<DataSource>> getDataSources(QueryType queryType) {
    List<DataSource> dataSources = new ArrayList<>();
    switch (queryType) {
      case PHENOTYPE:
        dataSources = phenotypeQueryService.getDataSources();
        break;
      case CONCEPT:
        dataSources = documentQueryService.getDataSources();
        break;
      default:
        phenotypeQueryService.getDataSources().addAll(documentQueryService.getDataSources());
        break;
    }
    return new ResponseEntity<>(dataSources, HttpStatus.OK);
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

  private QueryService getQueryService(String organisationId, String repositoryId, UUID queryId) {
    QueryType queryType =
        queryRepository
            .findByRepository_OrganisationIdAndRepositoryIdAndId(
                organisationId, repositoryId, String.valueOf(queryId))
            .orElseThrow()
            .getQueryType();
    switch (queryType) {
      case PHENOTYPE:
        return phenotypeQueryService;
      case CONCEPT:
        return documentQueryService;
      default:
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "No such query found.");
    }
  }
}
