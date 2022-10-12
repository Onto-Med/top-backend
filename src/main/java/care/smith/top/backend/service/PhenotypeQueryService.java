package care.smith.top.backend.service;

import care.smith.top.backend.model.QueryDao;
import care.smith.top.backend.model.QueryResultDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.repository.QueryRepository;
import care.smith.top.model.*;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.top_phenotypic_query.adapter.DataAdapter;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import care.smith.top.top_phenotypic_query.adapter.fhir.FHIRAdapter;
import care.smith.top.top_phenotypic_query.adapter.sql.SQLAdapter;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import care.smith.top.top_phenotypic_query.search.PhenotypeFinder;
import org.jetbrains.annotations.NotNull;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class PhenotypeQueryService {
  private static final Logger LOGGER = Logger.getLogger(PhenotypeQueryService.class.getName());

  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Value("${top.phenotyping.data-source-config-dir:config/data_sources}")
  private String dataSourceConfigDir;

  @Value("${top.phenotyping.execute-queries:true}")
  private boolean executeQueries;

  @Inject private JobScheduler jobScheduler;

  @Inject private StorageProvider storageProvider;

  @Autowired private RepositoryService repositoryService;
  @Autowired private EntityService entityService;
  @Autowired private QueryRepository queryRepository;

  public void deleteQuery(String organisationId, String repositoryId, UUID queryId) {
    if (!repositoryService.repositoryExists(organisationId, repositoryId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    QueryDao query =
        queryRepository
            .findByRepository_OrganisationIdAndRepositoryIdAndId(
                organisationId, repositoryId, queryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    queryRepository.delete(query);

    try {
      Job job = storageProvider.getJobById(queryId);
      storageProvider.deletePermanently(job.getId());
    } catch (Exception e) {
      LOGGER.fine(e.getMessage());
    }
  }

  public QueryResult enqueueQuery(String organisationId, String repositoryId, Query data) {
    RepositoryDao repository =
        repositoryService
            .getRepository(organisationId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (data == null
        || data.getId() == null
        || data.getDataSources() == null
        || data.getDataSources().isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    if (queryRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    if (getConfigs(data.getDataSources()).isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    jobScheduler.enqueue(data.getId(), () -> this.executeQuery(data.getId()));
    queryRepository.save(new QueryDao(data).repository(repository));

    return getQueryResult(organisationId, repositoryId, data.getId());
  }

  @org.jobrunr.jobs.annotations.Job(name = "Phenotypic query", retries = 0)
  public void executeQuery(UUID queryId) {
    OffsetDateTime createdAt = OffsetDateTime.now();
    QueryDao queryDao =
        queryRepository
            .findById(queryId)
            .orElseThrow(
                () ->
                    new NullPointerException(
                        String.format("Query with ID %s does not exist.", queryId)));

    LOGGER.info(
        String.format(
            "Running phenotypic query '%s' for repository '%s'...",
            queryId, queryDao.getRepository().getDisplayName()));

    DependentSubjectsMap phenotypes = new DependentSubjectsMap();
    phenotypes.putAll(
        entityService
            .getEntitiesByRepositoryId(
                queryDao.getRepository().getOrganisation().getId(),
                queryDao.getRepository().getId(),
                null,
                null,
                ApiModelMapper.phenotypeTypes(),
                null,
                null)
            .stream()
            .map(p -> (Phenotype) p)
            .collect(Collectors.toMap(Phenotype::getId, Function.identity())));
    Query query = queryDao.toApiModel();
    List<DataAdapterConfig> configs = getConfigs(query.getDataSources());

    // TODO: only one data source supported yet
    DataAdapterConfig config = configs.stream().findFirst().orElseThrow();

    // TODO: top-phenotypic-query does not derive adaptor type
    DataAdapter adapter = null;
    if (config.getConnectionAttribute("url") != null) adapter = new SQLAdapter(config);
    if (config.getConnectionAttribute("endpoint") != null) adapter = new FHIRAdapter(config);
    if (adapter == null) throw new NullPointerException("Adaptor type could not be derived.");

    QueryResultDao result;
    if (executeQueries) {
      try {
        // TODO: provide Writer to top-phenotypic-query and let it store the result set
        PhenotypeFinder finder = new PhenotypeFinder(query, phenotypes, adapter);
        ResultSet rs = finder.execute();
        adapter.close();
        result =
            new QueryResultDao(
                queryDao, createdAt, (long) rs.size(), OffsetDateTime.now(), QueryState.FINISHED);
      } catch (Throwable e) {
        e.printStackTrace();
        result =
            new QueryResultDao(queryDao, createdAt, null, OffsetDateTime.now(), QueryState.FAILED)
                .message(e.getMessage());
      }
    } else {
      result =
          new QueryResultDao(queryDao, createdAt, 0L, OffsetDateTime.now(), QueryState.FINISHED)
              .message("Query execution is disabled.");
    }

    queryDao.result(result);
    queryRepository.save(queryDao);
  }

  public Optional<DataAdapterConfig> getDataAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    return getDataAdapterConfigs().stream().filter(a -> id.equals(a.getId())).findFirst();
  }

  public QueryResult getQueryResult(String organisationId, String repositoryId, UUID queryId) {
    if (!repositoryService.repositoryExists(organisationId, repositoryId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    QueryDao query =
        queryRepository
            .findByRepository_OrganisationIdAndRepositoryIdAndId(
                organisationId, repositoryId, queryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (query.getResult() != null) return query.getResult().toApiModel();

    Job job = storageProvider.getJobById(queryId);

    QueryResult queryResult =
        new QueryResult()
            .id(queryId)
            .createdAt(job.getCreatedAt().atOffset(ZoneOffset.UTC))
            .state(getState(job));

    if (QueryState.FAILED.equals(queryResult.getState()))
      queryResult.finishedAt(job.getUpdatedAt().atOffset(ZoneOffset.UTC));
    return queryResult;
  }

  public List<DataAdapterConfig> getDataAdapterConfigs() {
    try (Stream<Path> paths = Files.list(Path.of(dataSourceConfigDir))) {
      return paths
          .map(this::toDataAdapterConfig)
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(DataAdapterConfig::getId))
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.warning(
          String.format("Could not load data adapter configs from dir '%s'.", dataSourceConfigDir));
    }
    return Collections.emptyList();
  }

  public List<DataSource> getDataSources() {
    return getDataAdapterConfigs().stream()
        .map(a -> new DataSource().id(a.getId()).title(a.getId().replace('_', ' ')))
        .sorted(Comparator.comparing(DataSource::getId))
        .collect(Collectors.toList());
  }

  public List<Query> getQueries(String organisationId, String repositoryId, Integer page) {
    PageRequest pageRequest = PageRequest.of(page == null ? 0 : page - 1, pageSize);
    return queryRepository
        .findAllByRepository_OrganisationIdAndRepositoryIdOrderByIdDesc(
            organisationId, repositoryId, pageRequest)
        .map(QueryDao::toApiModel)
        .getContent();
  }

  @NotNull
  private List<DataAdapterConfig> getConfigs(List<DataSource> dataSources) {
    return dataSources.stream()
        .map(s -> getDataAdapterConfig(s.getId()).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private QueryState getState(Job job) {
    if (job.hasState(StateName.FAILED) || job.hasState(StateName.DELETED)) {
      return QueryState.FAILED;
    } else if (job.hasState(StateName.SUCCEEDED)) {
      return QueryState.FINISHED;
    } else if (job.hasState(StateName.PROCESSING)) {
      return QueryState.RUNNING;
    }
    return QueryState.QUEUED;
  }

  private DataAdapterConfig toDataAdapterConfig(Path path) {
    try {
      DataAdapterConfig dataAdapterConfig = DataAdapterConfig.getInstance(path.toString());
      return dataAdapterConfig.getId() == null ? null : dataAdapterConfig;
    } catch (Exception e) {
      LOGGER.warning(
          String.format(
              "Data adapter config could not be loaded from file '%s'. Error: %s",
              path.toString(), e.getMessage()));
    }
    return null;
  }
}
