package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.model.jpa.QueryDao;
import care.smith.top.backend.model.jpa.QueryResultDao;
import care.smith.top.backend.model.jpa.RepositoryDao;
import care.smith.top.backend.repository.jpa.ConceptRepository;
import care.smith.top.backend.service.QueryService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.adapter.*;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import care.smith.top.top_document_query.converter.csv.DocumentCSV;
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
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentQueryService extends QueryService {
  private final Logger LOGGER = Logger.getLogger(DocumentQueryService.class.getName());

  private final DocumentCSV csvConverter = new DocumentCSV();

  @Value("${top.documents.data-source-config-dir:config/data_sources/nlp}")
  private String dataSourceConfigDir;

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
      List<DocumentHit> documents = finder.execute();
      result =
          new QueryResultDao(
              queryDao,
              createdAt,
              (long) documents.size(),
              OffsetDateTime.now(),
              QueryState.FINISHED);

      storeResult(
          queryDao.getRepository().getOrganisation().getId(),
          queryDao.getRepository().getId(),
          queryId.toString(),
          documents,
          concepts.toArray(new Concept[0]));
    } catch (Throwable e) {
      LOGGER.severe(e.getMessage());
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

    if (!repository.getOrganisation().hasDataSource(query.getDataSource()))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Data source does not exist for organisation!");

    if (getTextAdapterConfig(query.getDataSource()).isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Data source does not exist!");

    queryRepository.save(new QueryDao(query).repository(repository));
    jobScheduler.enqueue(queryId, () -> this.executeQuery(queryId));

    return getQueryById(organisationId, repositoryId, queryId).getResult();
  }

  public Optional<TextAdapterConfig> getTextAdapterConfig(String id) {
    if (id == null) return Optional.empty();
    return getTextAdapterConfigs().stream().filter(a -> id.equals(a.getId())).findFirst();
  }

  public List<String> getDocumentIds(String organisationId, String repositoryId, UUID queryId)
      throws IOException {
    Path queryPath =
        Paths.get(resultDir, organisationId, repositoryId, String.format("%s.zip", queryId));
    if (!queryPath.toFile().exists())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data for query found");
    if (!queryPath.startsWith(Paths.get(resultDir)))
      throw new FileSystemException("Repository directory isn't a child of the results directory.");

    ArrayList<String> ids = new ArrayList<>();
    try {
      ZipFile zip = new ZipFile(queryPath.toFile());
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry content = entries.nextElement();
        if (content.getName().equals("data.csv")) {
          ids.addAll(csvConverter.readFirstColumn(zip.getInputStream(content)));
        }
      }
      zip.close();
    } catch (IOException e) {
      LOGGER.warning(e.getMessage());
    }
    return ids;
  }

  public Optional<TextAdapter> getTextAdapter(
      String organisationId, String repositoryId, UUID queryId) {
    Query query = getQueryById(organisationId, repositoryId, queryId);
    TextAdapterConfig config = getTextAdapterConfig(query.getDataSource()).orElseThrow();
    try {
      return Optional.of(TextAdapter.getInstance(config));
    } catch (InstantiationException e) {
      return Optional.empty();
    }
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
      LOGGER.fine(
          String.format("Could not load text adapter configs from dir '%s'.", dataSourceConfigDir));
    }
    return Collections.emptyList();
  }

  public Optional<Path> getTextAdapterConfigPath(String id) {
    if (id == null) return Optional.empty();
    try (Stream<Path> paths =
        Files.list(Path.of(dataSourceConfigDir)).filter(f -> !Files.isDirectory(f))) {
      for (Path path : paths.collect(Collectors.toList())) {
        TextAdapterConfig config = toTextAdapterConfig(path);
        if (config != null && id.equals(config.getId())) return Optional.of(path);
      }
    } catch (Exception e) {
      LOGGER.fine(
          String.format("Could not load text adapter configs from dir '%s'.", dataSourceConfigDir));
    }
    return Optional.empty();
  }

  public List<DataSource> getDataSources() {
    return getTextAdapterConfigs().stream()
        .map(this::textAdapterConfigToDataSource)
        .sorted(Comparator.comparing(DataSource::getId))
        .collect(Collectors.toList());
  }

  private DataSource textAdapterConfigToDataSource(TextAdapterConfig textAdapterConfig) {
    return new DataSource()
        .id(textAdapterConfig.getId())
        .queryType(QueryType.CONCEPT)
        .title(textAdapterConfig.getId().replace('_', ' '));
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

  private void storeResult(
      String organisationId,
      String repositoryId,
      String queryId,
      List<DocumentHit> results,
      Concept[] concepts)
      throws IOException {

    ZipOutputStream zipStream = createZipStream(organisationId, repositoryId, queryId);

    zipStream.putNextEntry(new ZipEntry("metadata.csv"));
    csvConverter.write(concepts, zipStream);

    zipStream.putNextEntry(new ZipEntry("data.csv"));
    csvConverter.write(results, zipStream);

    zipStream.close();
  }
}
