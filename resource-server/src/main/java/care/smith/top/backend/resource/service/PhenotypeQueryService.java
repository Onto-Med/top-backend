package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.DataSource;
import care.smith.top.backend.model.Query;
import care.smith.top.backend.model.QueryResult;
import care.smith.top.backend.model.QueryState;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PhenotypeQueryService {
  private static final Logger LOGGER = Logger.getLogger(PhenotypeQueryService.class.getName());

  @Inject private JobScheduler jobScheduler;
  @Inject private StorageProvider storageProvider;

  @Value("${top.phenotyping.data-source-config-dir:config/data_sources}")
  private String dataSourceConfigDir;

  public UUID enqueueQuery(Query query) {
    if (query == null
        || query.getId() == null
        || query.getConfiguration() == null
        || query.getConfiguration().getSources() == null
        || query.getConfiguration().getSources().isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    List<DataAdapterConfig> configs =
        query.getConfiguration().getSources().stream()
            .map(s -> getDataAdapterConfig(s.getId()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    if (configs.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    return jobScheduler
        .enqueue(
            query.getId(),
            () -> {
              // TODO: call method from top-phenotypic-query package
              System.out.println("Enqueueing query job.");
            })
        .asUUID();
  }

  public Optional<DataAdapterConfig> getDataAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    return getDataAdapterConfigs().stream().filter(a -> id.equals(a.getId())).findFirst();
  }

  public List<DataAdapterConfig> getDataAdapterConfigs() {
    try (Stream<Path> paths = Files.list(Path.of(dataSourceConfigDir))) {
      return paths
          .map(this::toDataAdapterConfig)
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(DataAdapterConfig::getId))
          .collect(Collectors.toList());
    } catch (Exception ignored) {
    }
    return Collections.emptyList();
  }

  public List<DataSource> getDataSources() {
    return getDataAdapterConfigs().stream()
        .map(a -> new DataSource().id(a.getId()).title(a.getId().replace('_', ' ')))
        .sorted(Comparator.comparing(DataSource::getId))
        .collect(Collectors.toList());
  }

  public QueryResult getQueryResult(UUID queryId) {
    Job job = storageProvider.getJobById(queryId);
    if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    QueryResult queryResult =
        new QueryResult()
            .id(queryId)
            .createdAt(job.getCreatedAt().atOffset(ZoneOffset.UTC))
            .state(getState(job));

    if (QueryState.FINISHED.equals(queryResult.getState())) {
      // TODO: get total count of subjects in query result
      queryResult.finishedAt(job.getUpdatedAt().atOffset(ZoneOffset.UTC)).count(0L);
    }
    return queryResult;
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
