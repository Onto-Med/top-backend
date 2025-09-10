package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.EntityDao;
import care.smith.top.backend.model.jpa.OrganisationDao;
import care.smith.top.backend.model.jpa.RepositoryDao;
import care.smith.top.backend.model.jpa.RepositoryDao_;
import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.ExpectedResultDao;
import care.smith.top.backend.repository.jpa.OrganisationRepository;
import care.smith.top.backend.repository.jpa.PhenotypeRepository;
import care.smith.top.backend.repository.jpa.RepositoryRepository;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.ExpectedResultRepository;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.result.PhenotypeValues;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepositoryService implements ContentService {
  private static final Logger LOGGER = Logger.getLogger(RepositoryService.class.getName());

  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Value("${top.phenotyping.result.dir:config/query_results}")
  private String resultDir;

  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private OrganisationRepository organisationRepository;
  @Autowired private EncounterRepository encounterRepository;
  @Autowired private ExpectedResultRepository expectedResultRepository;
  @Autowired private PhenotypeRepository phenotypeRepository;
  @Autowired private UserService userService;
  @Autowired private PhenotypeQueryService phenotypeQueryService;

  @Override
  public long count() {
    return repositoryRepository.count();
  }

  @Transactional
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public Repository createRepository(String organisationId, Repository data, List<String> include) {
    if (repositoryRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);

    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Organisation '%s' does not exist!", organisationId)));

    RepositoryDao repository = new RepositoryDao(data).organisation(organisation);

    if (repository.getRepositoryType() == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Repository requires a repositoryType!");
    return repositoryRepository.save(repository).toApiModel();
  }

  @Transactional
  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'WRITE')")
  public void deleteRepository(String repositoryId, String organisationId, List<String> include) {
    repositoryRepository.delete(
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));

    Path repositoryPath = Paths.get(resultDir, organisationId, repositoryId);
    if (!repositoryPath.startsWith(Paths.get(resultDir)))
      LOGGER.severe(
          String.format(
              "Repository directory '%s' is invalid and cannot be deleted!", repositoryPath));
    try {
      Files.deleteIfExists(repositoryPath);
    } catch (IOException e) {
      LOGGER.severe(
          String.format(
              "Could not delete repository directory '%s'! Cause: %s",
              repositoryPath, e.getMessage()));
    }
  }

  @Transactional
  public Page<Repository> getRepositories(
      List<String> include,
      String name,
      Boolean primary,
      RepositoryType repositoryType,
      Integer page) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by(RepositoryDao_.NAME));
    return repositoryRepository
        .findByOrganisationIdAndNameAndPrimaryAndRepositoryType(
            null, name, primary, repositoryType, userService.getCurrentUser(), pageRequest)
        .map(r -> r.toApiModel(userService.getCurrentUser()));
  }

  public Page<Repository> getRepositoriesByOrganisationId(
      String organisationId,
      List<String> include,
      String name,
      RepositoryType repositoryType,
      Integer page) {
    PageRequest pageRequest =
        PageRequest.of(page == null ? 0 : page - 1, pageSize, Sort.by(RepositoryDao_.NAME));
    return repositoryRepository
        .findByOrganisationIdAndNameAndPrimaryAndRepositoryType(
            organisationId, name, null, repositoryType, userService.getCurrentUser(), pageRequest)
        .map(RepositoryDao::toApiModel);
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
  public Repository getRepository(
      String organisationId, String repositoryId, List<String> include) {
    return repositoryRepository
        .findByIdAndOrganisationId(repositoryId, organisationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .toApiModel();
  }

  @Transactional
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'WRITE')")
  public Repository updateRepository(
      String organisationId, String repositoryId, Repository data, List<String> include) {
    RepositoryDao repository =
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return repositoryRepository.saveAndFlush(repository.update(data)).toApiModel();
  }

  @Transactional
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'WRITE')")
  public List<TestReport> testRepository(
      String organisationId, String repositoryId, String dataSourceId) {
    RepositoryDao repository =
        repositoryRepository
            .findByIdAndOrganisationId(repositoryId, organisationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!RepositoryType.PHENOTYPE_REPOSITORY.equals(repository.getRepositoryType()))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Only phenotype repositories can be tested!");

    if (!repository.getOrganisation().hasDataSource(dataSourceId))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Data source does not exist for organisation!");

    List<String> phenotypes =
        phenotypeRepository
            .findAllByRepositoryId(repositoryId, Pageable.unpaged())
            .map(EntityDao::getId)
            .toList();

    List<ExpectedResultDao> expectedResults =
        expectedResultRepository.findAllByExpectedResultKeyDataSourceId(dataSourceId).stream()
            .filter(er -> phenotypes.contains(er.getPhenotypeId()))
            .toList();

    if (expectedResults.isEmpty()) return new ArrayList<>();

    List<String> projectionIds =
        expectedResults.stream().map(ExpectedResultDao::getPhenotypeId).distinct().toList();
    List<ProjectionEntry> projection =
        projectionIds.stream()
            .map(id -> new ProjectionEntry(id, ProjectionEntry.TypeEnum.PROJECTION_ENTRY))
            .toList();

    PhenotypeQuery query =
        new PhenotypeQuery(UUID.randomUUID(), QueryType.PHENOTYPE, dataSourceId)
            .projection(projection);

    ResultSet resultSet;
    try {
      resultSet = phenotypeQueryService.executeQuery(query, repositoryId);
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Could not test repository. Query has failed for the provided test data.");
    }

    List<TestReport> reports =
        new ArrayList<>(
            expectedResults.stream()
                .map(
                    er -> {
                      DateTimeRestriction dateRange =
                          er.getEncounter() != null ? er.getEncounter().toDateRange() : null;
                      List<care.smith.top.model.Value> values =
                          Objects.requireNonNullElse(
                                  resultSet.getValues(er.getEncounterId(), er.getPhenotypeId()),
                                  new PhenotypeValues(er.getPhenotypeId()))
                              .getValues(dateRange);

                      care.smith.top.model.Value actual = null;
                      if (values != null) {
                        actual = values.stream().findFirst().orElse(null);
                      }

                      return er.toReport(actual);
                    })
                .toList());

    List<TestReport> remaining =
        resultSet.getPhenotypes().stream()
            .flatMap(
                p ->
                    p.values().stream()
                        .filter(v -> projectionIds.contains(v.getPhenotypeName()))
                        .flatMap(v -> toTestReport(dataSourceId, p.getSubjectId(), v)))
            .sorted(Comparator.comparing(TestReport::getSubjectId))
            .toList();

    for (TestReport a : remaining) {
      boolean isPresent =
          reports.stream()
              .noneMatch(
                  e ->
                      (e.getSubjectId() == null
                              || Objects.equals(a.getSubjectId(), e.getSubjectId()))
                          && (e.getEncounterId() == null
                              || Objects.equals(a.getEncounterId(), e.getEncounterId()))
                          && Objects.equals(a.getEntityId(), e.getEntityId())
                          && isEqual(a.getActual(), e.getActual()));
      if (isPresent) reports.add(a);
    }

    return reports;
  }

  private boolean isEqual(care.smith.top.model.Value a, care.smith.top.model.Value b) {
    if (Objects.equals(a, b)) return true;
    if (a == null || b == null || a.getDataType() != b.getDataType()) return false;

    return switch (a.getDataType()) {
      case STRING -> Objects.equals(((StringValue) a).getValue(), ((StringValue) b).getValue());
      case NUMBER -> Objects.equals(((NumberValue) a).getValue(), ((NumberValue) b).getValue());
      case BOOLEAN -> Objects.equals(((BooleanValue) a).isValue(), ((BooleanValue) b).isValue());
      case DATE_TIME ->
          Objects.equals(((DateTimeValue) a).getValue(), ((DateTimeValue) b).getValue());
    };
  }

  private Stream<TestReport> toTestReport(
      String dataSourceId, String encounterId, PhenotypeValues phenotypeValues) {
    Optional<EncounterDao> encounter =
        encounterRepository.findByEncounterKeyDataSourceIdAndEncounterKeyEncounterId(
            dataSourceId, encounterId);
    return phenotypeValues.values().stream()
        .flatMap(
            v ->
                v.stream()
                    .map(
                        w ->
                            new TestReport(
                                    null,
                                    phenotypeValues.getPhenotypeName(),
                                    encounter.map(EncounterDao::getSubjectId).orElse(null),
                                    encounterId,
                                    null)
                                .actual(w)));
  }
}
