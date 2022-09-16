package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.DataSource;
import care.smith.top.backend.model.Query;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
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
            () -> {
              // TODO: call method from top-phenotypic-query package
              System.out.println("Enqueueing query job.");
            })
        .asUUID();
  }

  public ResultSet getQueryResult(UUID jobId) {
    Job job = storageProvider.getJobById(jobId);
    if (job == null || job.hasState(StateName.FAILED) || job.hasState(StateName.DELETED)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } else if (job.hasState(StateName.SUCCEEDED)) {
      // TODO: retriev job result
      return new ResultSet();
    }
    // TODO: backoff for x seconds
    return null;
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
