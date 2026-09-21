package care.smith.top.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import care.smith.top.backend.model.jpa.OrganisationDao;
import care.smith.top.backend.model.jpa.OrganisationDataSourceDao;
import care.smith.top.backend.model.jpa.datasource.DataSourceDao;
import care.smith.top.backend.repository.jpa.OrganisationRepository;
import care.smith.top.backend.repository.jpa.QueryRepository;
import care.smith.top.backend.repository.jpa.datasource.*;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.PhenotypeQueryService;
import care.smith.top.backend.service.QueryService;
import care.smith.top.backend.service.datasource.*;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.service.nlp.QueryExpansionService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import care.smith.top.top_document_query.adapter.config.QueryExpansionConfig;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileEntity;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileRelationEntity;
import java.io.*;
import java.nio.file.FileSystemException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QueryApiDelegateImpl implements QueryApiDelegate {
  private static final ObjectMapper SNAKE_CASE_OBJECT_MAPPER =
      new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  @Autowired private PhenotypeQueryService phenotypeQueryService;
  @Autowired private DocumentQueryService documentQueryService;
  @Autowired private QueryExpansionService queryExpansionService;
  @Autowired private OrganisationService organisationService;
  @Autowired private QueryRepository queryRepository;
  @Autowired private SubjectRepository subjectRepository;
  @Autowired private EncounterRepository encounterRepository;
  @Autowired private SubjectResourceRepository subjectResourceRepository;
  @Autowired private ExpectedResultRepository expectedResultRepository;
  @Autowired private DataSourceRepository dataSourceRepository;
  @Autowired private OrganisationRepository organisationRepository;

  @Override
  public ResponseEntity<Void> deleteQuery(
      String organisationId, String repositoryId, UUID queryId) {
    getQueryService(organisationId, repositoryId, queryId)
        .deleteQuery(organisationId, repositoryId, queryId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Resource> downloadQueryResult(
      String organisationId, String repositoryId, UUID queryId) {
    try {
      File file =
          getQueryService(organisationId, repositoryId, queryId)
              .getQueryResultPath(organisationId, repositoryId, queryId)
              .toFile();
      ContentDisposition contentDisposition =
          ContentDisposition.builder("inline").filename(file.getName()).build();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentDisposition(contentDisposition);
      return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    } catch (FileSystemException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Query result is not accessible.", e);
    }
  }

  @Override
  public ResponseEntity<QueryResult> enqueueQuery(
      String organisationId, String repositoryId, Query query) {

    return switch (query.getType()) {
      case PHENOTYPE ->
          new ResponseEntity<>(
              phenotypeQueryService.enqueueQuery(organisationId, repositoryId, query),
              HttpStatus.CREATED);
      case CONCEPT ->
          new ResponseEntity<>(
              documentQueryService.enqueueQuery(organisationId, repositoryId, query),
              HttpStatus.CREATED);
    };
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<DataSource>> getDataSources(QueryType queryType) {
    return new ResponseEntity<>(new ArrayList<>(loadDataSources(queryType)), HttpStatus.OK);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public ResponseEntity<Void> deleteDataSource(String dataSourceId) {
    dataSourceRepository.deleteById(dataSourceId);
    subjectRepository.deleteAllBySubjectKeyDataSourceId(dataSourceId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<QueryExpansionEffectiveConfig> getDataSourceQueryExpansionConfig(
      String dataSourceId) {
    QueryExpansionContext context = getQueryExpansionContext(dataSourceId);
    return ResponseEntity.ok(
        new QueryExpansionEffectiveConfig()
            .profile(context.profileName())
            .relations(toApiRelations(context.effectiveRelations())));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<QueryExpansionProfile>> getQueryExpansionProfiles() {
    List<QueryExpansionProfile> profiles =
        queryExpansionService
            .getProfiles()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Query-expansion profiles not found or Concept Graphs API unavailable."))
            .stream()
            .map(this::toApiProfile)
            .collect(Collectors.toList());
    return ResponseEntity.ok(profiles);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<QueryExpansionProfile> getQueryExpansionProfile(String profileName) {
    QueryExpansionProfileEntity profile =
        queryExpansionService
            .getProfile(profileName)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Query-expansion profile not found or Concept Graphs API unavailable."));
    return ResponseEntity.ok(toApiProfile(profile));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<QueryExpansionResponse> expandQueryForDataSource(
      String dataSourceId, QueryExpansionRequest request) {
    QueryExpansionContext context = getQueryExpansionContext(dataSourceId);
    Map<String, Object> rawRequest = SNAKE_CASE_OBJECT_MAPPER.convertValue(request, Map.class);
    List<String> allowedRelationIds =
        context.effectiveRelations().stream()
            .map(QueryExpansionProfileRelationEntity::getId)
            .collect(Collectors.toList());
    List<Map<String, Object>> relationDefinitions =
        context.effectiveRelations().stream()
            .map(this::toConceptGraphsRelationDefinition)
            .collect(Collectors.toList());

    Map<String, Object> rawResponse =
        queryExpansionService
            .expand(rawRequest, context.profileName(), allowedRelationIds, relationDefinitions)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Query expansion failed or Concept Graphs API unavailable."));

    return ResponseEntity.ok(
        SNAKE_CASE_OBJECT_MAPPER.convertValue(rawResponse, QueryExpansionResponse.class));
  }

  @Override
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'MANAGE')")
  @Transactional
  public ResponseEntity<Void> uploadDataSource(
      String organisationId,
      MultipartFile file,
      DataSourceFileType fileType,
      String dataSourceId,
      String config) {
    OrganisationDao organisation =
        organisationRepository
            .findById(organisationId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organisation does not exist."));
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
      DataImport.getInstance(
              subjectRepository,
              encounterRepository,
              subjectResourceRepository,
              expectedResultRepository,
              reader,
              fileType,
              dataSourceId,
              Stream.of(
                      "subjectId",
                      "birthDate",
                      "sex",
                      "encounterId",
                      "type",
                      "startDateTime",
                      "endDateTime",
                      "subjectResourceId",
                      "codeSystem",
                      "code",
                      "dateTime",
                      "unit",
                      "numberValue",
                      "textValue",
                      "booleanValue",
                      "dateTimeValue",
                      "expectedResultId",
                      "phenotypeId")
                  .map(c -> c + "=" + c)
                  .collect(Collectors.joining(";")))
          .run();
      dataSourceRepository.save(new DataSourceDao(dataSourceId));
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          String.format("Could not read uploaded file. (%s)", e.getMessage()));
    }
    try {
      OrganisationDataSourceDao dataSourceDao =
          new OrganisationDataSourceDao(organisation, dataSourceId, QueryType.PHENOTYPE);
      organisationRepository.save(organisation.addDataSource(dataSourceDao));
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          String.format("Could not add data source to organisation '%s'.", organisation.getName()));
    }
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> addOrganisationDataSource(
      String organisationId, DataSource dataSource) {
    organisationService.addOrganisationDataSource(organisationId, dataSource);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'READ')")
  @Override
  public ResponseEntity<List<DataSource>> getOrganisationDataSources(
      String organisationId, QueryType queryType) {
    Collection<String> ids =
        organisationService.getOrganisationDataSourceIds(organisationId, queryType);
    return ResponseEntity.ok(
        loadDataSources(queryType).stream()
            .filter(ds -> ids.contains(ds.getId()))
            .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<Void> removeOrganisationDataSource(
      String organisationId, DataSource dataSource) {
    organisationService.removeOrganisationDataSource(organisationId, dataSource);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<QueryPage> getQueries(
      String organisationId, String repositoryId, Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toQueryPage(
            phenotypeQueryService.getQueries(organisationId, repositoryId, page)));
  }

  @Override
  public ResponseEntity<Query> getQueryById(
      String organisationId, String repositoryId, UUID queryId) {
    return new ResponseEntity<>(
        getQueryService(organisationId, repositoryId, queryId)
            .getQueryById(organisationId, repositoryId, queryId),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Map<String, List<String>>> getQueryResultIds(
      String organisationId, String repositoryId, UUID queryId) {
    try {
      if (getQueryType(organisationId, repositoryId, queryId) == QueryType.CONCEPT) {
        return ResponseEntity.ok(
            documentQueryService.getDocumentIdsAndOffsets(organisationId, repositoryId, queryId));
      } else {
        // ToDo: this should not be reached by non-ConceptQueries, but maybe another response
        // necessary
        return ResponseEntity.ok(Map.of());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private QueryExpansionContext getQueryExpansionContext(String dataSourceId) {
    TextAdapterConfig adapterConfig =
        documentQueryService
            .getTextAdapterConfig(dataSourceId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found."));
    QueryExpansionConfig queryExpansion = adapterConfig.getQueryExpansion();
    if (queryExpansion == null || queryExpansion.getProfile() == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Data source has no query-expansion configuration.");
    }

    QueryExpansionProfileEntity profile =
        queryExpansionService
            .getProfile(queryExpansion.getProfile())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Query-expansion profile not found or Concept Graphs API unavailable."));

    Set<String> configuredRelationIds = queryExpansion.getRelations().keySet();
    List<QueryExpansionProfileRelationEntity> effectiveRelations =
        profile.getRelations().stream()
            .filter(relation -> configuredRelationIds.contains(relation.getId()))
            .collect(Collectors.toList());
    return new QueryExpansionContext(queryExpansion.getProfile(), profile, effectiveRelations);
  }

  private QueryExpansionProfile toApiProfile(QueryExpansionProfileEntity profile) {
    QueryExpansionProfile apiProfile =
        new QueryExpansionProfile()
            .name(profile.getName())
            .languageName(getLanguageName(profile))
            .categories(getProfileCategories(profile))
            .defaultCategories(getDefaultCategories(profile))
            .relations(toApiRelations(profile.getRelations()));
    setDefaultRelations(apiProfile, getDefaultRelations(profile));
    return apiProfile;
  }

  private String getLanguageName(QueryExpansionProfileEntity profile) {
    return invokeStringGetter(profile, "getLanguageName");
  }

  private List<QueryExpansionProfileCategory> getProfileCategories(
      QueryExpansionProfileEntity profile) {
    try {
      Object categories = profile.getClass().getMethod("getCategories").invoke(profile);
      if (categories instanceof List<?> categoryList) {
        return categoryList.stream().map(this::toApiCategory).collect(Collectors.toList());
      }
    } catch (ReflectiveOperationException ignored) {
      // Older top-document-query snapshots did not expose categories yet.
    }
    return Collections.emptyList();
  }

  private List<String> getDefaultCategories(QueryExpansionProfileEntity profile) {
    try {
      Object defaultCategories = profile.getClass().getMethod("getDefaultCategories").invoke(profile);
      if (defaultCategories instanceof List<?> categoryList) {
        return categoryList.stream().filter(String.class::isInstance).map(String.class::cast).toList();
      }
    } catch (ReflectiveOperationException ignored) {
      // Older top-document-query snapshots did not expose default categories yet.
    }
    return Collections.emptyList();
  }

  private List<String> getDefaultRelations(QueryExpansionProfileEntity profile) {
    try {
      Object defaultRelations = profile.getClass().getMethod("getDefaultRelations").invoke(profile);
      if (defaultRelations instanceof List<?> relationList) {
        Set<String> profileRelationIds =
            profile.getRelations().stream()
                .map(QueryExpansionProfileRelationEntity::getId)
                .collect(Collectors.toSet());
        return relationList.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(profileRelationIds::contains)
            .toList();
      }
    } catch (ReflectiveOperationException ignored) {
      // Older top-document-query snapshots did not expose default relations yet.
    }
    return Collections.emptyList();
  }

  private void setDefaultRelations(
      QueryExpansionProfile profile, List<String> defaultRelations) {
    try {
      profile.getClass().getMethod("defaultRelations", List.class).invoke(profile, defaultRelations);
    } catch (ReflectiveOperationException ignored) {
      // Older top-api snapshots did not expose default relations yet.
    }
  }

  private QueryExpansionProfileCategory toApiCategory(Object category) {
    return new QueryExpansionProfileCategory()
        .id(invokeStringGetter(category, "getId"))
        .description(invokeStringGetter(category, "getDescription"));
  }

  private String invokeStringGetter(Object target, String methodName) {
    try {
      Object value = target.getClass().getMethod(methodName).invoke(target);
      return value instanceof String stringValue ? stringValue : null;
    } catch (ReflectiveOperationException ignored) {
      return null;
    }
  }

  private List<QueryExpansionRelation> toApiRelations(
      List<QueryExpansionProfileRelationEntity> relations) {
    return relations.stream().map(this::toApiRelation).collect(Collectors.toList());
  }

  private QueryExpansionRelation toApiRelation(QueryExpansionProfileRelationEntity relation) {
    return new QueryExpansionRelation()
        .id(relation.getId())
        .label(relation.getLabel())
        .description(relation.getDescription())
        .sourceCategories(relation.getSourceCategories())
        .targetCategories(relation.getTargetCategories());
  }

  private Map<String, Object> toConceptGraphsRelationDefinition(
      QueryExpansionProfileRelationEntity relation) {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("id", relation.getId());
    definition.put("description", relation.getDescription());
    definition.put("source_categories", relation.getSourceCategories());
    definition.put("target_categories", relation.getTargetCategories());
    return definition;
  }

  private record QueryExpansionContext(
      String profileName,
      QueryExpansionProfileEntity profile,
      List<QueryExpansionProfileRelationEntity> effectiveRelations) {}

  private QueryService getQueryService(String organisationId, String repositoryId, UUID queryId) {
    return switch (getQueryType(organisationId, repositoryId, queryId)) {
      case PHENOTYPE -> phenotypeQueryService;
      case CONCEPT -> documentQueryService;
    };
  }

  private QueryType getQueryType(String organisationId, String repositoryId, UUID queryId) {
    return queryRepository
        .findByRepository_OrganisationIdAndRepositoryIdAndId(
            organisationId, repositoryId, String.valueOf(queryId))
        .orElseThrow()
        .getQueryType();
  }

  private Collection<DataSource> loadDataSources(QueryType queryType) {
    List<DataSource> dataSources = new ArrayList<>();
    if (queryType == null || QueryType.PHENOTYPE.equals(queryType))
      dataSources.addAll(phenotypeQueryService.getDataSources());
    if (queryType == null || QueryType.CONCEPT.equals(queryType))
      dataSources.addAll(documentQueryService.getDataSources());
    return dataSources;
  }
}
