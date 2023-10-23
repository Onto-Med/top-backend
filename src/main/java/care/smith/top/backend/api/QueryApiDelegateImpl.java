package care.smith.top.backend.api;

import care.smith.top.backend.repository.jpa.QueryRepository;
import care.smith.top.backend.service.PhenotypeQueryService;
import care.smith.top.backend.service.QueryService;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
          getQueryService(organisationId, repositoryId, queryId)
              .getQueryResultPath(organisationId, repositoryId, queryId)
              .toFile();
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
            HttpStatus.NOT_ACCEPTABLE,
            String.format(
                "Query type is not valid; must be one of [%s]",
                Arrays.stream(QueryType.values())
                    .map(QueryType::toString)
                    .collect(Collectors.joining(", "))));
    }
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
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
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> addOrganisationDataSource(String organisationId, String body) {
    return QueryApiDelegate.super.addOrganisationDataSource(organisationId, body);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<DataSource>> getOrganisationDataSources(String organisationId) {
    return QueryApiDelegate.super.getOrganisationDataSources(organisationId);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeOrganisationDataSource(String organisationId, String body) {
    return QueryApiDelegate.super.removeOrganisationDataSource(organisationId, body);
  }

  @Override
  public ResponseEntity<QueryPage> getQueries(
      String organisationId, String repositoryId, Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toQueryPage(
            phenotypeQueryService.getQueries(organisationId, repositoryId, page)));
  }

  @Override
  public ResponseEntity<Query> getQueryById(
      String organisationId, String repositoryId, UUID queryId) {
    return new ResponseEntity<>(
        getQueryService(organisationId, repositoryId, queryId)
            .getQueryById(organisationId, repositoryId, queryId),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<String>> getQueryResultIds(
      String organisationId, String repositoryId, UUID queryId) {
    try {
      if (getQueryType(organisationId, repositoryId, queryId) == QueryType.CONCEPT) {
        return ResponseEntity.ok(
            documentQueryService.getDocumentIds(organisationId, repositoryId, queryId));
      } else {
        // ToDo: this should not be reached by non-ConceptQueries, but maybe another response
        // necessary
        return ResponseEntity.ok(new ArrayList<>());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private QueryService getQueryService(String organisationId, String repositoryId, UUID queryId) {
    switch (getQueryType(organisationId, repositoryId, queryId)) {
      case PHENOTYPE:
        return phenotypeQueryService;
      case CONCEPT:
        return documentQueryService;
      default:
        throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "No such query found.");
    }
  }

  private QueryType getQueryType(String organisationId, String repositoryId, UUID queryId) {
    return queryRepository
        .findByRepository_OrganisationIdAndRepositoryIdAndId(
            organisationId, repositoryId, String.valueOf(queryId))
        .orElseThrow()
        .getQueryType();
  }
}
