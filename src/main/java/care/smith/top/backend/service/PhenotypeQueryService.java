package care.smith.top.backend.service;

import care.smith.top.backend.model.QueryDao;
import care.smith.top.backend.model.QueryResultDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.repository.PhenotypeRepository;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.adapter.DataAdapter;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import care.smith.top.top_phenotypic_query.converter.csv.CSV;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import care.smith.top.top_phenotypic_query.search.PhenotypeFinder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhenotypeQueryService extends QueryService {
  private final Logger LOGGER = Logger.getLogger(PhenotypeQueryService.class.getName());

  private final CSV csvConverter = new CSV();

  @Value("${top.phenotyping.data-source-config-dir:config/data_sources}")
  private String dataSourceConfigDir;

  @Value("${top.phenotyping.execute-queries:true}")
  private boolean executeQueries;

  @Value("${top.phenotyping.result.dir:config/query_results}")
  private String resultDir;

  @Value("${top.phenotyping.result.download-enabled:true}")
  private boolean queryResultDownloadEnabled;

  @Autowired private PhenotypeRepository phenotypeRepository;

  @Override
  public QueryResult enqueueQuery(String organisationId, String repositoryId, Query query) {
    if (!(query instanceof PhenotypeQuery))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "The provided query is not a phenotype query!");

    RepositoryDao repository =
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (query.getId() == null || query.getDataSources() == null || query.getDataSources().isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    UUID queryId = query.getId();

    if (queryRepository.existsById(queryId.toString()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    if (getConfigs(query.getDataSources()).isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    queryRepository.save(new QueryDao(query).repository(repository));
    jobScheduler.enqueue(queryId, () -> this.executeQuery(queryId));

    return getQueryResult(organisationId, repositoryId, queryId);
  }

  @Override
  @org.jobrunr.jobs.annotations.Job(name = "Phenotypic query", retries = 0)
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

    Entity[] phenotypes =
        phenotypeRepository
            .findAllByRepositoryIdAndEntityTypeIn(
                queryDao.getRepository().getId(),
                ApiModelMapper.phenotypeTypes(),
                Pageable.unpaged())
            .map(e -> (Phenotype) e.toApiModel())
            .getContent()
            .toArray(new Phenotype[0]);
    PhenotypeQuery query = (PhenotypeQuery) queryDao.toApiModel();
    List<DataAdapterConfig> configs = getConfigs(query.getDataSources());

    // TODO: only one data source supported yet
    DataAdapterConfig config = configs.stream().findFirst().orElseThrow();

    QueryResultDao result;
    try {
      ResultSet rs;
      if (executeQueries) {
        DataAdapter adapter = DataAdapter.getInstance(config);
        if (adapter == null) throw new NullPointerException("Adaptor type could not be derived.");

        PhenotypeFinder finder = new PhenotypeFinder(query, phenotypes, adapter);
        rs = finder.execute();
        adapter.close();
        result =
            new QueryResultDao(
                queryDao, createdAt, (long) rs.size(), OffsetDateTime.now(), QueryState.FINISHED);
      } else {
        rs = new ResultSet();
        result =
            new QueryResultDao(queryDao, createdAt, 0L, OffsetDateTime.now(), QueryState.FINISHED)
                .message("Query execution is disabled.");
      }

      storeResult(
          queryDao.getRepository().getOrganisation().getId(),
          queryDao.getRepository().getId(),
          queryId.toString(),
          rs,
          phenotypes);
    } catch (Throwable e) {
      e.printStackTrace();
      result =
          new QueryResultDao(queryDao, createdAt, null, OffsetDateTime.now(), QueryState.FAILED)
              .message("Cause: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
    }

    queryDao.result(result);
    queryRepository.save(queryDao);
  }

  public Optional<DataAdapterConfig> getDataAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    return getDataAdapterConfigs().stream().filter(a -> id.equals(a.getId())).findFirst();
  }

  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public Path getQueryResultPath(String organisationId, String repositoryId, UUID queryId)
      throws FileSystemException {
    if (!queryResultDownloadEnabled)
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Query result download is disabled.");
    if (!repositoryRepository.existsByIdAndOrganisation_Id(repositoryId, organisationId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository does not exist.");

    QueryDao query =
        queryRepository
            .findByRepository_OrganisationIdAndRepositoryIdAndId(
                organisationId, repositoryId, queryId.toString())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Query does not exist."));

    if (query.getResult() == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Query has no result.");

    Path queryPath =
        Paths.get(resultDir, organisationId, repositoryId, String.format("%s.zip", queryId));
    if (!queryPath.startsWith(Paths.get(resultDir)))
      throw new FileSystemException("Repository directory isn't a child of the results directory.");

    return queryPath;
  }

  public List<DataAdapterConfig> getDataAdapterConfigs() {
    try (Stream<Path> paths =
        Files.list(Path.of(dataSourceConfigDir)).filter(f -> !Files.isDirectory(f))) {
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

  @NotNull
  private List<DataAdapterConfig> getConfigs(List<String> dataSources) {
    return dataSources.stream()
        .map(s -> getDataAdapterConfig(s).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private void storeResult(
      String organisationId,
      String repositoryId,
      String queryId,
      ResultSet resultSet,
      Entity[] phenotypes)
      throws IOException {
    Path repositoryPath = Paths.get(resultDir, organisationId, repositoryId);
    if (!repositoryPath.startsWith(Paths.get(resultDir)))
      throw new FileSystemException("Repository directory isn't a child of the results directory.");

    Files.createDirectories(repositoryPath);
    File zipFile =
        Files.createFile(repositoryPath.resolve(String.format("%s.zip", queryId))).toFile();
    ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile));

    zipStream.putNextEntry(new ZipEntry("data.csv"));
    csvConverter.write(resultSet, zipStream);

    zipStream.putNextEntry(new ZipEntry("metadata.csv"));
    csvConverter.write(phenotypes, zipStream);

    zipStream.close();
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

  protected void clearResults(String organisationId, String repositoryId, String queryId)
      throws IOException {
    Path queryPath =
        Paths.get(resultDir, organisationId, repositoryId, String.format("%s.zip", queryId));
    if (!queryPath.startsWith(Paths.get(resultDir)))
      LOGGER.severe(String.format("Query file '%s' is invalid and cannot be deleted!", queryPath));
    Files.deleteIfExists(queryPath);
  }
}
