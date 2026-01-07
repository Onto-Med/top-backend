package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.model.jpa.datasource.DataSourceDao;
import care.smith.top.backend.repository.jpa.CodeRepository;
import care.smith.top.backend.repository.jpa.PhenotypeRepository;
import care.smith.top.backend.repository.jpa.datasource.DataSourceRepository;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.adapter.DataAdapter;
import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import care.smith.top.top_phenotypic_query.adapter.sql.SQLAdapterDataSource;
import care.smith.top.top_phenotypic_query.converter.csv.CSV;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import care.smith.top.top_phenotypic_query.search.PhenotypeFinder;
import care.smith.top.top_phenotypic_query.util.Entities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;
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

  @Value("${spring.datasource.url}")
  private String jpaDataSourceUrl;

  @Value("${spring.datasource.username}")
  private String jpaDataSourceUsername;

  @Value("${spring.datasource.password}")
  private String jpaDataSourcePassword;

  @Autowired private PhenotypeRepository phenotypeRepository;
  @Autowired private DataSourceRepository dataSourceRepository;
  @Autowired private CodeRepository codeRepository;

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

    if (!repository.getOrganisation().hasDataSource(query.getDataSource()))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Data source does not exist for organisation!");

    if (getDataAdapterConfig(query.getDataSource()).isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Data source does not exist!");

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

    PhenotypeQuery query = (PhenotypeQuery) queryDao.toApiModel();
    QueryResultDao result;
    try {
      ResultSet rs;
      if (executeQueries) {
        rs = executeQuery(query, queryDao.getRepository().getId());
        result =
            new QueryResultDao(
                queryDao, createdAt, (long) rs.size(), OffsetDateTime.now(), QueryState.FINISHED);
      } else {
        rs = new ResultSet();
        result =
            new QueryResultDao(queryDao, createdAt, 0L, OffsetDateTime.now(), QueryState.FINISHED)
                .message("Query execution is disabled.");
      }
      storeResult(queryDao, rs);
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      result =
          new QueryResultDao(queryDao, createdAt, null, OffsetDateTime.now(), QueryState.FAILED)
              .message("Cause: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
    }

    queryDao.result(result);
    queryRepository.save(queryDao);
  }

  public ResultSet executeQuery(PhenotypeQuery query, String repositoryId)
      throws InstantiationException, SQLException, Entities.NoCodesException {
    Entity[] phenotypes = getQueryRelatedPhenotypes(query, repositoryId).toArray(Entity[]::new);

    DataAdapterConfig config = getDataAdapterConfig(query.getDataSource()).orElseThrow();

    ResultSet resultSet = new ResultSet();
    if (executeQueries) {
      DataAdapter adapter = DataAdapter.getInstance(config);
      if (adapter == null) throw new NullPointerException("Adaptor type could not be derived.");

      PhenotypeFinder finder = new PhenotypeFinder(query, phenotypes, adapter);
      resultSet = finder.execute();
      adapter.close();
    }

    return resultSet;
  }

  public Optional<DataAdapterConfig> getDataAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    Optional<DataSourceDao> dataSource = dataSourceRepository.findById(id);
    if (dataSource.isPresent()) {
      DataAdapterConfig config = new DataAdapterConfig();
      config.setAdapter(SQLAdapterDataSource.class.getName());
      config.setId(id);
      config.setConnectionAttribute("url", jpaDataSourceUrl);
      config.setConnectionAttribute("user", jpaDataSourceUsername);
      config.setConnectionAttribute("password", jpaDataSourcePassword);
      return Optional.of(config);
    }
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
      LOGGER.fine(
          String.format("Could not load data adapter configs from dir '%s'.", dataSourceConfigDir));
    }
    return Collections.emptyList();
  }

  public List<DataSource> getDataSources() {
    return Stream.concat(
            dataSourceRepository.findAll().stream().map(DataSourceDao::toApiModel),
            getDataAdapterConfigs().stream().map(this::dataAdapterConfigToDataSource))
        .sorted(Comparator.comparing(DataSource::getId))
        .collect(Collectors.toList());
  }

  private DataSource dataAdapterConfigToDataSource(DataAdapterConfig dataAdapterConfig) {
    return new DataSource()
        .id(dataAdapterConfig.getId())
        .queryType(QueryType.PHENOTYPE)
        .title(dataAdapterConfig.getId().replace('_', ' '));
  }

  private List<Entity> getQueryRelatedPhenotypes(PhenotypeQuery query, String repositoryId) {
    return Stream.concat(
            Objects.requireNonNullElse(query.getProjection(), new ArrayList<ProjectionEntry>())
                .stream()
                .map(ProjectionEntry::getSubjectId),
            Objects.requireNonNullElse(query.getCriteria(), new ArrayList<QueryCriterion>())
                .stream()
                .map(QueryCriterion::getSubjectId))
        .distinct()
        .flatMap(
            id ->
                phenotypeRepository
                    .findByIdAndRepositoryId(id, repositoryId)
                    .map(
                        entityDao -> {
                          Set<EntityDao> result = phenotypeRepository.getDependencies(entityDao);
                          result.add(entityDao);
                          return result.stream();
                        })
                    .orElse(null))
        .filter(Objects::nonNull)
        .map(EntityService.prepareEntity(codeRepository))
        .toList();
  }

  private void storeResult(QueryDao queryDao, ResultSet resultSet) throws IOException {
    ZipOutputStream zipStream =
        createZipStream(
            queryDao.getRepository().getOrganisation().getId(),
            queryDao.getRepository().getId(),
            queryDao.getId());

    Entity[] phenotypes =
        getQueryRelatedPhenotypes(
                (PhenotypeQuery) queryDao.toApiModel(), queryDao.getRepository().getId())
            .toArray(new Entity[0]);

    zipStream.putNextEntry(new ZipEntry("metadata.csv"));
    csvConverter.writeMetadata(phenotypes, zipStream);

    zipStream.putNextEntry(new ZipEntry("data_phenotypes.csv"));
    csvConverter.writePhenotypes(resultSet, phenotypes, zipStream);

    zipStream.putNextEntry(new ZipEntry("data_subjects.csv"));
    csvConverter.writeSubjects(
        resultSet, phenotypes, (PhenotypeQuery) queryDao.toApiModel(), zipStream);

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
}
