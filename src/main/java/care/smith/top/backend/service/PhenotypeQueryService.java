package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.repository.jpa.PhenotypeRepository;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.adapter.DataAdapter;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import care.smith.top.top_phenotypic_query.converter.csv.CSV;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import care.smith.top.top_phenotypic_query.search.PhenotypeFinder;
import java.io.IOException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    if (!isValid(query)) throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    UUID queryId = query.getId();

    if (queryRepository.existsById(queryId.toString()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    if (getDataAdapterConfig(query.getDataSource()).isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);

    queryRepository.save(new QueryDao(query).repository(repository));
    jobScheduler.enqueue(queryId, () -> this.executeQuery(queryId));

    return getQueryById(organisationId, repositoryId, queryId).getResult();
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
        Stream.concat(
                queryDao.getProjection().stream().map(ProjectionEntryDao::getSubjectId),
                queryDao.getCriteria().stream().map(QueryCriterionDao::getSubjectId))
            .distinct()
            .flatMap(
                id ->
                    phenotypeRepository
                        .findByIdAndRepositoryId(id, queryDao.getRepository().getId())
                        .map(
                            entityDao -> {
                              Set<EntityDao> result =
                                  phenotypeRepository.getDependencies(entityDao);
                              result.add(entityDao);
                              return result.stream();
                            })
                        .orElse(null))
            .filter(Objects::nonNull)
            .map(EntityDao::toApiModel)
            .toArray(Entity[]::new);

    PhenotypeQuery query = (PhenotypeQuery) queryDao.toApiModel();
    DataAdapterConfig config = getDataAdapterConfig(query.getDataSource()).orElseThrow();

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

  private void storeResult(
      String organisationId,
      String repositoryId,
      String queryId,
      ResultSet resultSet,
      Entity[] phenotypes)
      throws IOException {
    ZipOutputStream zipStream = createZipStream(organisationId, repositoryId, queryId);

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
