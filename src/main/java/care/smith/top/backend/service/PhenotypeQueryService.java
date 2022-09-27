package care.smith.top.backend.service;

import care.smith.top.model.*;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PhenotypeQueryService {
  private static final Logger LOGGER = Logger.getLogger(PhenotypeQueryService.class.getName());

  @Inject
  private JobScheduler jobScheduler;

  @Inject private StorageProvider storageProvider;

  @Autowired
  private            RepositoryService repositoryService;
  @Autowired private EntityService     entityService;

  @Value("${top.phenotyping.data-source-config-dir:config/data_sources}")
  private String dataSourceConfigDir;

  public void deleteQuery(String organisationId, String repositoryId, UUID queryId) {
    if (!repositoryService.repositoryExists(organisationId, repositoryId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    Job job = storageProvider.getJobById(queryId);
    if (!repositoryId.equals(job.getJobDetails().getJobParameters().get(1).getObject()))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    storageProvider.deletePermanently(job.getId());
    // TODO: cleanup stored query results
  }

  public QueryResult enqueueQuery(String organisationId, String repositoryId, Query query) {
    if (!repositoryService.repositoryExists(organisationId, repositoryId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    if (query == null
      || query.getId() == null
      || query.getDataSources() == null
      || query.getDataSources().isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    List<DataAdapterConfig> configs =
      query.getDataSources().stream()
        .map(s -> getDataAdapterConfig(s.getId()).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    if (configs.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    jobScheduler
      .enqueue(
        query.getId(), () -> this.executeQuery(organisationId, repositoryId, query.getId()))
      .asUUID();

    return getQueryResult(organisationId, repositoryId, query.getId());
  }

  @org.jobrunr.jobs.annotations.Job(name = "Phenotypic query", retries = 0)
  public void executeQuery(String organisationId, String repositoryId, UUID queryId) {
    LOGGER.info(
      String.format(
        "Running phenotypic query '%s' for repository '%s'...", queryId, repositoryId));

    DependentSubjectsMap phenotypes = new DependentSubjectsMap();
    phenotypes.putAll(
      entityService
        .getEntitiesByRepositoryId(
          organisationId,
          repositoryId,
          null,
          null,
          ApiModelMapper.phenotypeTypes(),
          null,
          null)
        .stream()
        .map(p -> (Phenotype) p)
        .collect(Collectors.toMap(Phenotype::getId, Function.identity())));
    // query.dependentSubjects(phenotypes);
    // TODO: call method from top-phenotypic-query package
  }

  public Optional<DataAdapterConfig> getDataAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    return getDataAdapterConfigs().stream().filter(a -> id.equals(a.getId())).findFirst();
  }

  public QueryResult getQueryResult(String organisationId, String repositoryId, UUID queryId) {
    if (!repositoryService.repositoryExists(organisationId, repositoryId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    Job job = storageProvider.getJobById(queryId);
    if (!repositoryId.equals(job.getJobDetails().getJobParameters().get(1).getObject()))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    QueryResult queryResult =
      new QueryResult()
        .id(queryId)
        .createdAt(job.getCreatedAt().atOffset(ZoneOffset.UTC))
        .state(getState(job));

    if (QueryState.FINISHED.equals(queryResult.getState())) {
      // TODO: get total count of subjects in query result
      queryResult.finishedAt(job.getUpdatedAt().atOffset(ZoneOffset.UTC)).count(0L);
    } else if (QueryState.FAILED.equals(queryResult.getState())) {
      queryResult.finishedAt(job.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
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
