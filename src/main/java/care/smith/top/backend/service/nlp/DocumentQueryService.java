package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.QueryDao;
import care.smith.top.backend.model.QueryResultDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.service.QueryService;
import care.smith.top.model.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentQueryService extends QueryService {
  private final Logger LOGGER = Logger.getLogger(DocumentQueryService.class.getName());

  @Value("${top.documents.data-source-config-dir:config/data_sources/nlp}")
  private String dataSourceConfigDir;

  @Autowired private DocumentRepository documentRepository;

  @Override
  @org.jobrunr.jobs.annotations.Job(name = "Document query", retries = 0)
  public void executeQuery(UUID queryId) {
    OffsetDateTime createdAt = OffsetDateTime.now();
    QueryDao queryDao =
        queryRepository
            .findById(queryId.toString())
            .orElseThrow(
                () ->
                    new NullPointerException(
                        String.format("Query with ID %s does not exist.", queryId)));

    LOGGER.info(
        String.format(
            "Running %s query '%s' for repository '%s'...",
            getClass().getSimpleName(), queryId, queryDao.getRepository().getDisplayName()));

    ConceptQuery query = (ConceptQuery) queryDao.toApiModel();

    // TODO: implement query logic

    QueryResultDao result =
        new QueryResultDao(queryDao, createdAt, 0L, OffsetDateTime.now(), QueryState.FINISHED);

    queryDao.result(result);
    queryRepository.save(queryDao);
  }

  @Override
  public QueryResult enqueueQuery(String organisationId, String repositoryId, Query query) {
    if (!(query instanceof ConceptQuery))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "The provided query is not a concept query!");

    RepositoryDao repository =
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (query.getId() == null || query.getDataSources() == null || query.getDataSources().isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    UUID queryId = query.getId();

    if (queryRepository.existsById(queryId.toString()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    // TODO: create any prerequisites

    queryRepository.save(new QueryDao(query).repository(repository));
    jobScheduler.enqueue(queryId, () -> this.executeQuery(queryId));

    return getQueryResult(organisationId, repositoryId, queryId);
  }

  @Override
  protected void clearResults(String organisationId, String repositoryId, String queryId)
      throws Exception {}
}
