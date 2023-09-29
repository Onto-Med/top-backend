package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.Permission;
import care.smith.top.backend.model.jpa.QueryDao;
import care.smith.top.backend.repository.jpa.QueryRepository;
import care.smith.top.backend.repository.jpa.RepositoryRepository;
import care.smith.top.model.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Transactional
public abstract class QueryService {
  private final Logger LOGGER = Logger.getLogger(QueryService.class.getName());

  @Value("${spring.paging.page-size:10}")
  protected int pageSize;

  @Value("${top.result.dir:config/query_results}")
  protected String resultDir;

  @Value("${top.result.download-enabled:true}")
  protected boolean queryResultDownloadEnabled;

  @Autowired protected JobScheduler jobScheduler;
  @Autowired protected StorageProvider storageProvider;
  @Autowired protected QueryRepository queryRepository;
  @Autowired protected RepositoryRepository repositoryRepository;

  /**
   * Enqueues the given query to the {@link JobScheduler}.
   *
   * @param organisationId ID of the organisation the query belongs to.
   * @param repositoryId ID of the repository the query belongs to.
   * @param query The query specification.
   * @return A {@link QueryResult} that reflects the state immediately after enqueuing.
   */
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public abstract QueryResult enqueueQuery(String organisationId, String repositoryId, Query query);

  /**
   * Executes a given query without retries.
   *
   * @param queryId ID of the query to be executed.
   */
  public abstract void executeQuery(UUID queryId);

  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
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

  /**
   * Deletes a query. The delete request is propagated to the underlying {@link JobScheduler}.
   *
   * <p>If authentication is enabled, users are required to have {@link Permission#WRITE} permission
   * for the organisation.
   *
   * @param organisationId ID of the organisation the query belongs to.
   * @param repositoryId ID of the repository the query belongs to.
   * @param queryId ID of the query to be deleted.
   */
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public void deleteQuery(String organisationId, String repositoryId, UUID queryId) {
    if (!repositoryRepository.existsByIdAndOrganisation_Id(repositoryId, organisationId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    QueryDao query =
        queryRepository
            .findByRepository_OrganisationIdAndRepositoryIdAndId(
                organisationId, repositoryId, queryId.toString())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    try {
      Job job = storageProvider.getJobById(queryId);
      storageProvider.deletePermanently(job.getId());
    } catch (Exception e) {
      LOGGER.fine(e.getMessage());
    }

    try {
      clearResults(
          query.getRepository().getOrganisation().getId(),
          query.getRepository().getId(),
          queryId.toString());
    } catch (Exception e) {
      LOGGER.warning(e.getMessage());
    }

    queryRepository.delete(query);
  }

  /**
   * Returns the result description of a query. If the query is still running, the result will only
   * contain a creation timestamp and a state.
   *
   * <p>If authentication is enabled, users are required to have {@link Permission#WRITE} permission
   * for the organisation.
   *
   * @param organisationId ID of the organisation the query belongs to.
   * @param repositoryId ID of the repository the query belongs to.
   * @param queryId ID of the query for which a result description is requested.
   * @return Description of the query's result.
   */
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public QueryResult getQueryResult(String organisationId, String repositoryId, UUID queryId) {
    if (!repositoryRepository.existsByIdAndOrganisation_Id(repositoryId, organisationId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    QueryDao query =
        queryRepository
            .findByRepository_OrganisationIdAndRepositoryIdAndId(
                organisationId, repositoryId, queryId.toString())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (query.getResult() != null) return query.getResult().toApiModel();

    QueryResult queryResult = new QueryResult().id(queryId);

    try {
      Job job = storageProvider.getJobById(queryId);
      queryResult.createdAt(job.getCreatedAt().atOffset(ZoneOffset.UTC)).state(getState(job));
      if (QueryState.FAILED.equals(queryResult.getState()))
        queryResult.finishedAt(job.getUpdatedAt().atOffset(ZoneOffset.UTC));
    } catch (JobNotFoundException ignored) {
    }

    return queryResult;
  }

  /**
   * Returns a page of queries that belong to the given repository.
   *
   * <p>If authentication is enabled, users are required to have {@link Permission#WRITE} permission
   * for the organisation.
   *
   * @param organisationId ID of the organisation the repository belongs to.
   * @param repositoryId ID of the repository for which queries are requested.
   * @param page Page number, starting at 1.
   * @return A {@link care.smith.top.model.Page} containing queries.
   */
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public Page<Query> getQueries(String organisationId, String repositoryId, Integer page) {
    PageRequest pageRequest = PageRequest.of(page == null ? 0 : page - 1, pageSize);
    return queryRepository
        .findAllByRepository_OrganisationIdAndRepositoryIdOrderByCreatedAtDesc(
            organisationId, repositoryId, pageRequest)
        .map(QueryDao::toApiModel);
  }

  private boolean isEmpty(Collection<?> list) {
    return list == null || list.isEmpty();
  }

  /**
   * Deletes all other results related to the given query (such as files in the file system or
   * additional database records).
   *
   * @param organisationId ID of the organisation the query belongs to.
   * @param repositoryId ID of the repository the query belongs to.
   * @param queryId ID of the query for which results shall be deleted.
   * @throws Exception If some result artifacts could not be deleted.
   */
  protected abstract void clearResults(String organisationId, String repositoryId, String queryId)
      throws Exception;

  /**
   * Extracts the state of a job.
   *
   * <p>A {@link Job} uses {@link StateName} to indicate it's state. This state is converted to
   * {@link QueryState} in the following way:
   *
   * <ul>
   *   <li>{@link StateName#FAILED}, {@link StateName#DELETED} => {@link QueryState#FAILED}
   *   <li>{@link StateName#SUCCEEDED} => {@link QueryState#FINISHED}
   *   <li>{@link StateName#PROCESSING} => {@link QueryState#RUNNING}
   * </ul>
   *
   * @param job The job to extract the state from.
   * @return A {@link QueryState}.
   */
  protected QueryState getState(Job job) {
    if (job.hasState(StateName.FAILED) || job.hasState(StateName.DELETED)) {
      return QueryState.FAILED;
    } else if (job.hasState(StateName.SUCCEEDED)) {
      return QueryState.FINISHED;
    } else if (job.hasState(StateName.PROCESSING)) {
      return QueryState.RUNNING;
    }
    return QueryState.QUEUED;
  }

  protected boolean isValid(Query query) {
    return query != null
        && query.getId() != null
        && query.getDataSource() != null
        && (QueryType.CONCEPT.equals(query.getType())
                && ((ConceptQuery) query).getEntityId() != null
            || QueryType.PHENOTYPE.equals(query.getType())
                && (!isEmpty(((PhenotypeQuery) query).getCriteria())
                    || !isEmpty(((PhenotypeQuery) query).getProjection())));
  }

  protected ZipOutputStream createZipStream(
      String organisationId, String repositoryId, String queryId) throws IOException {
    Path repositoryPath = Paths.get(resultDir, organisationId, repositoryId);
    if (!repositoryPath.startsWith(Paths.get(resultDir)))
      throw new FileSystemException("Repository directory isn't a child of the results directory.");

    Files.createDirectories(repositoryPath);
    File zipFile =
        Files.createFile(repositoryPath.resolve(String.format("%s.zip", queryId))).toFile();
    return new ZipOutputStream(new FileOutputStream(zipFile));
  }
}
