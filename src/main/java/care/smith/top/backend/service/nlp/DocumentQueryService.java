package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.model.jpa.QueryDao;
import care.smith.top.backend.model.jpa.QueryResultDao;
import care.smith.top.backend.model.jpa.RepositoryDao;
import care.smith.top.backend.repository.elasticsearch.DocumentRepository;
import care.smith.top.backend.repository.jpa.ConceptRepository;
import care.smith.top.backend.service.QueryService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.adapter.ElasticDocument;
import care.smith.top.top_document_query.adapter.TextAdapter;
import care.smith.top.top_document_query.adapter.TextAdapterConfig;
import care.smith.top.top_document_query.adapter.TextFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  @Autowired private ConceptRepository conceptRepository;

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

    EntityDao entity =
        conceptRepository
            .findByIdAndRepositoryId(queryDao.getEntityId(), queryDao.getRepository().getId())
            .orElseThrow();
    List<Entity> concepts =
        conceptRepository.getDependencies(entity).stream()
            .map(EntityDao::toApiModel)
            .collect(Collectors.toList());
    concepts.add(entity.toApiModel());

    TextAdapterConfig config = getTextAdapterConfig(query.getDataSource()).orElseThrow();
    QueryResultDao result;
    try {
      TextAdapter adapter = TextAdapter.getInstance(config);
      TextFinder finder = new TextFinder(query, concepts.toArray(new Entity[0]), adapter);
      List<ElasticDocument> documents = finder.execute();
      result =
          new QueryResultDao(
              queryDao,
              createdAt,
              (long) documents.size(),
              OffsetDateTime.now(),
              QueryState.FINISHED);
    } catch (InstantiationException e) {
      e.printStackTrace();
      result =
          new QueryResultDao(queryDao, createdAt, null, OffsetDateTime.now(), QueryState.FAILED)
              .message("Cause: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
    }

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

    if (!isValid(query)) throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    UUID queryId = query.getId();

    if (queryRepository.existsById(queryId.toString()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    if (getTextAdapterConfig(query.getDataSource()).isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    queryRepository.save(new QueryDao(query).repository(repository));
    jobScheduler.enqueue(queryId, () -> this.executeQuery(queryId));

    return getQueryResult(organisationId, repositoryId, queryId);
  }

  public Optional<TextAdapterConfig> getTextAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    return getTextAdapterConfigs().stream().filter(a -> id.equals(a.getId())).findFirst();
  }

  public List<TextAdapterConfig> getTextAdapterConfigs() {
    try (Stream<Path> paths =
        Files.list(Path.of(dataSourceConfigDir)).filter(f -> !Files.isDirectory(f))) {
      return paths
          .map(this::toTextAdapterConfig)
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(TextAdapterConfig::getId))
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.warning(
          String.format("Could not load text adapter configs from dir '%s'.", dataSourceConfigDir));
    }
    return Collections.emptyList();
  }

  public List<DataSource> getDataSources() {
    return getTextAdapterConfigs().stream()
        .map(a -> new DataSource().id(a.getId()).title(a.getId().replace('_', ' ')))
        .sorted(Comparator.comparing(DataSource::getId))
        .collect(Collectors.toList());
  }

  private TextAdapterConfig toTextAdapterConfig(Path path) {
    try {
      TextAdapterConfig textAdapterConfig = TextAdapterConfig.getInstance(path.toString());
      return textAdapterConfig.getId() == null ? null : textAdapterConfig;
    } catch (Exception e) {
      LOGGER.warning(
          String.format(
              "Text adapter config could not be loaded from file '%s'. Error: %s",
              path.toString(), e.getMessage()));
    }
    return null;
  }

  @Override
  protected void clearResults(String organisationId, String repositoryId, String queryId)
      throws Exception {}
}
