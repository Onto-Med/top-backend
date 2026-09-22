package care.smith.top.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.service.nlp.QueryExpansionService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.adapter.config.QueryExpansionConfig;
import care.smith.top.top_document_query.adapter.config.QueryExpansionRelationConfig;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileEntity;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileRelationEntity;
import care.smith.top.top_document_query.query_expansion.QueryExpansionExpressionCompiler;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QueryExpansionApiDelegateImpl implements QueryExpansionApiDelegate {
  private static final ObjectMapper SNAKE_CASE_OBJECT_MAPPER =
      new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  @Autowired private DocumentQueryService documentQueryService;
  @Autowired private QueryExpansionService queryExpansionService;

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
    List<Map<String, Object>> relationMappings = getRelationMappings(rawRequest);
    List<QueryExpansionProfileRelationEntity> requestedRelations =
        getRequestedRelations(context.effectiveRelations(), relationMappings);
    List<String> allowedRelationIds =
        requestedRelations.stream()
            .map(QueryExpansionProfileRelationEntity::getId)
            .distinct()
            .collect(Collectors.toList());
    List<Map<String, Object>> relationDefinitions =
        toConceptGraphsRelationDefinitions(requestedRelations, relationMappings);

    Map<String, Object> rawResponse =
        queryExpansionService
            .expand(rawRequest, context.profileName(), allowedRelationIds, relationDefinitions)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Query expansion failed or Concept Graphs API unavailable."));

    QueryExpansionResponse response =
        SNAKE_CASE_OBJECT_MAPPER.convertValue(
            normalizeSnakeCaseKeys(rawResponse), QueryExpansionResponse.class);
    response.generatedEntityDrafts(createGeneratedEntityDrafts(rawRequest, response, context));
    return ResponseEntity.ok(response);
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
    return new QueryExpansionContext(
        queryExpansion.getProfile(), profile, effectiveRelations, queryExpansion);
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
        return categoryList.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
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

  private void setDefaultRelations(QueryExpansionProfile profile, List<String> defaultRelations) {
    try {
      profile.getClass().getMethod("defaultRelations", List.class).invoke(profile, defaultRelations);
    } catch (ReflectiveOperationException ignored) {
      // Older top-api snapshots did not expose default relations yet.
    }
  }

  private QueryExpansionProfileCategory toApiCategory(Object category) {
    String id = invokeStringGetter(category, "getId");
    String label = invokeStringGetter(category, "getLabel");
    QueryExpansionProfileCategory apiCategory =
        new QueryExpansionProfileCategory()
            .id(id)
            .description(invokeStringGetter(category, "getDescription"));
    setCategoryLabel(apiCategory, label == null || label.isBlank() ? id : label);
    return apiCategory;
  }

  private void setCategoryLabel(QueryExpansionProfileCategory category, String label) {
    try {
      category.getClass().getMethod("label", String.class).invoke(category, label);
    } catch (ReflectiveOperationException ignored) {
      // Older top-api snapshots did not expose category labels yet.
    }
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

  private List<Map<String, Object>> getRelationMappings(Map<String, Object> rawRequest) {
    Object mappings = rawRequest.get("relationMappings");
    if (mappings == null) mappings = rawRequest.get("relation_mappings");
    if (!(mappings instanceof List<?> mappingList)) return Collections.emptyList();
    return mappingList.stream()
        .filter(Map.class::isInstance)
        .map(mapping -> (Map<String, Object>) mapping)
        .collect(Collectors.toList());
  }

  private List<QueryExpansionProfileRelationEntity> getRequestedRelations(
      List<QueryExpansionProfileRelationEntity> effectiveRelations,
      List<Map<String, Object>> relationMappings) {
    if (relationMappings.isEmpty()) return Collections.emptyList();
    Set<String> requestedRelationIds =
        relationMappings.stream()
            .map(this::getRelationMappingRelationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return effectiveRelations.stream()
        .filter(relation -> requestedRelationIds.contains(relation.getId()))
        .collect(Collectors.toList());
  }

  private List<Map<String, Object>> toConceptGraphsRelationDefinitions(
      List<QueryExpansionProfileRelationEntity> requestedRelations,
      List<Map<String, Object>> relationMappings) {
    Map<String, QueryExpansionProfileRelationEntity> relationById =
        requestedRelations.stream()
            .collect(Collectors.toMap(QueryExpansionProfileRelationEntity::getId, relation -> relation));

    if (relationMappings.isEmpty()) {
      return requestedRelations.stream()
          .map(
              relation ->
                  toConceptGraphsRelationDefinition(
                      relation, relation.getSourceCategories(), relation.getTargetCategories()))
          .collect(Collectors.toList());
    }

    return relationMappings.stream()
        .map(
            mapping -> {
              String relationId = getRelationMappingRelationId(mapping);
              QueryExpansionProfileRelationEntity relation = relationById.get(relationId);
              if (relation == null) return null;
              List<String> sourceCategoryIds =
                  filterAllowedCategories(
                      getStringList(mapping, "sourceCategoryIds", "source_category_ids"),
                      relation.getSourceCategories());
              List<String> targetCategoryIds =
                  filterAllowedCategories(
                      getStringList(mapping, "targetCategoryIds", "target_category_ids"),
                      relation.getTargetCategories());
              if (sourceCategoryIds.isEmpty() || targetCategoryIds.isEmpty()) return null;
              return toConceptGraphsRelationDefinition(relation, sourceCategoryIds, targetCategoryIds);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String getRelationMappingRelationId(Map<String, Object> mapping) {
    Object relationId = mapping.get("relationId");
    if (relationId == null) relationId = mapping.get("relation_id");
    return relationId instanceof String stringValue ? stringValue : null;
  }

  private List<String> getStringList(Map<String, Object> mapping, String camelKey, String snakeKey) {
    Object value = mapping.get(camelKey);
    if (value == null) value = mapping.get(snakeKey);
    if (!(value instanceof List<?> valueList)) return Collections.emptyList();
    return valueList.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }

  private List<String> filterAllowedCategories(List<String> requested, List<String> allowed) {
    Set<String> allowedSet = new HashSet<>(allowed);
    return requested.stream().filter(allowedSet::contains).distinct().toList();
  }

  private Map<String, Object> toConceptGraphsRelationDefinition(
      QueryExpansionProfileRelationEntity relation,
      List<String> sourceCategoryIds,
      List<String> targetCategoryIds) {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("id", relation.getId());
    definition.put("description", relation.getDescription());
    definition.put("source_categories", sourceCategoryIds);
    definition.put("target_categories", targetCategoryIds);
    return definition;
  }

  private QueryExpansionGeneratedEntityDrafts createGeneratedEntityDrafts(
      Map<String, Object> rawRequest,
      QueryExpansionResponse response,
      QueryExpansionContext context) {
    String sourceConceptId = getString(rawRequest, "sourceConceptId", "source_concept_id");
    String language = response.getLanguage() == null ? "en" : response.getLanguage();
    SingleConcept sourceConcept = sourceConceptId == null ? null : conceptRef(sourceConceptId);

    Map<String, String> draftIdByConceptGraphId = new HashMap<>();
    Map<String, String> draftTitleById = new HashMap<>();
    if (sourceConceptId != null)
      draftTitleById.put(sourceConceptId, getString(rawRequest, "term", "term"));
    Map<String, SingleConcept> categoryDraftsByCategory = new LinkedHashMap<>();
    Map<String, SingleConcept> termDraftsByKey = new LinkedHashMap<>();

    if (response.getConcepts() != null) {
      for (QueryExpansionConcept concept : response.getConcepts()) {
        SingleConcept categoryDraft =
            getOrCreateCategoryDraft(
                concept.getCategory(), language, sourceConcept, categoryDraftsByCategory, context);
        SingleConcept draft = createSingleConceptDraft(getConceptTitle(concept), language, categoryDraft);
        termDraftsByKey.put(concept.getId(), draft);
        draftIdByConceptGraphId.put(concept.getId(), draft.getId());
        draftTitleById.put(draft.getId(), getTitleText(draft));
      }
    }

    if (termDraftsByKey.isEmpty() && response.getExpansions() != null) {
      response
          .getExpansions()
          .values()
          .forEach(
              candidates ->
                  candidates.forEach(
                      candidate -> {
                        String term = candidate.getTerm();
                        if (term == null || term.isBlank()) return;
                        SingleConcept categoryDraft =
                            getOrCreateCategoryDraft(
                                candidate.getCategory(),
                                language,
                                sourceConcept,
                                categoryDraftsByCategory,
                                context);
                        String key = candidate.getCategory() + ":" + term.toLowerCase();
                        termDraftsByKey.putIfAbsent(
                            key, createSingleConceptDraft(term, language, categoryDraft));
                        SingleConcept draft = termDraftsByKey.get(key);
                        draftTitleById.put(draft.getId(), getTitleText(draft));
                      }));
    }

    categoryDraftsByCategory
        .values()
        .forEach(draft -> draftTitleById.put(draft.getId(), getTitleText(draft)));
    List<SingleConcept> singleDrafts = new ArrayList<>();
    singleDrafts.addAll(categoryDraftsByCategory.values());
    singleDrafts.addAll(termDraftsByKey.values());
    List<CompositeConcept> compositeDrafts =
        createCompositeConceptDrafts(
            response,
            context,
            sourceConceptId,
            draftIdByConceptGraphId,
            termDraftsByKey,
            draftTitleById,
            language,
            sourceConcept);
    return new QueryExpansionGeneratedEntityDrafts()
        .singleConcepts(singleDrafts)
        .compositeConcepts(compositeDrafts);
  }

  private SingleConcept getOrCreateCategoryDraft(
      String category,
      String language,
      SingleConcept sourceConcept,
      Map<String, SingleConcept> categoryDraftsByCategory,
      QueryExpansionContext context) {
    String categoryId = category == null || category.isBlank() ? "category" : category;
    return categoryDraftsByCategory.computeIfAbsent(
        categoryId,
        id -> createSingleConceptDraft(getCategoryLabel(id, context), language, sourceConcept));
  }

  private SingleConcept createSingleConceptDraft(String title, String language, SingleConcept superConcept) {
    SingleConcept draft =
        new SingleConcept()
            .id(UUID.randomUUID().toString())
            .entityType(EntityType.SINGLE_CONCEPT)
            .titles(List.of(localisableText(language, title)));
    if (superConcept != null) draft.superConcepts(List.of(superConcept));
    return draft;
  }

  private String getConceptTitle(QueryExpansionConcept concept) {
    if (concept.getTerms() != null && !concept.getTerms().isEmpty()) {
      return concept.getTerms().get(0);
    }
    if (concept.getLabel() != null && !concept.getLabel().isBlank()) return concept.getLabel();
    return "Query expansion concept";
  }

  private String getCategoryLabel(String categoryId, QueryExpansionContext context) {
    if (context.profile().getCategories() == null) return categoryId;
    return context.profile().getCategories().stream()
        .filter(category -> categoryId.equals(category.getId()))
        .map(
            category ->
                category.getLabel() == null || category.getLabel().isBlank()
                    ? category.getId()
                    : category.getLabel())
        .findFirst()
        .orElse(categoryId);
  }

  private List<CompositeConcept> createCompositeConceptDrafts(
      QueryExpansionResponse response,
      QueryExpansionContext context,
      String sourceConceptId,
      Map<String, String> draftIdByConceptGraphId,
      Map<String, SingleConcept> singleDraftsByKey,
      Map<String, String> draftTitleById,
      String language,
      SingleConcept sourceConcept) {
    if (response.getRelations() == null) return Collections.emptyList();

    Map<String, QueryExpansionRelationConfig> relationConfigById = context.config().getRelations();
    return response.getRelations().stream()
        .map(
            relation -> {
              String resolvedSourceDraftId = draftIdByConceptGraphId.get(relation.getSourceConceptId());
              if (resolvedSourceDraftId == null) resolvedSourceDraftId = sourceConceptId;
              String targetDraftId = draftIdByConceptGraphId.get(relation.getTargetConceptId());
              if (targetDraftId == null && singleDraftsByKey.size() == 1) {
                targetDraftId = singleDraftsByKey.values().iterator().next().getId();
              }
              QueryExpansionRelationConfig relationConfig = relationConfigById.get(relation.getRelation());
              if (resolvedSourceDraftId == null
                  || targetDraftId == null
                  || relationConfig == null
                  || relationConfig.getStrategy() == null) {
                return null;
              }
              String finalSourceDraftId = resolvedSourceDraftId;
              String finalTargetDraftId = targetDraftId;
              return QueryExpansionExpressionCompiler
                  .compileRelation(relationConfig.getStrategy(), finalSourceDraftId, finalTargetDraftId)
                  .map(
                      expression ->
                          createCompositeConceptDraft(
                              getRelationLabel(relation.getRelation(), context),
                              finalSourceDraftId,
                              finalTargetDraftId,
                              getDisplayTitle(finalSourceDraftId, draftTitleById),
                              getDisplayTitle(finalTargetDraftId, draftTitleById),
                              expression,
                              language,
                              sourceConcept))
                  .orElse(null);
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private CompositeConcept createCompositeConceptDraft(
      String relationLabel,
      String sourceConceptId,
      String targetConceptId,
      String sourceTitle,
      String targetTitle,
      Expression expression,
      String language,
      SingleConcept sourceConcept) {
    CompositeConcept draft =
        new CompositeConcept()
            .id(UUID.randomUUID().toString())
            .entityType(EntityType.COMPOSITE_CONCEPT)
            .titles(
                List.of(
                    localisableText(
                        language, sourceTitle + " --" + relationLabel + "--> " + targetTitle)))
            .expression(expression);
    if (sourceConcept != null) draft.superConcepts(List.of(sourceConcept));
    draft.descriptions(
        List.of(
            localisableText(
                language, sourceTitle + " --" + relationLabel + "--> " + targetTitle)));
    return draft;
  }

  private String getRelationLabel(String relationId, QueryExpansionContext context) {
    return context.effectiveRelations().stream()
        .filter(relation -> relationId.equals(relation.getId()))
        .map(QueryExpansionProfileRelationEntity::getLabel)
        .filter(Objects::nonNull)
        .filter(label -> !label.isBlank())
        .findFirst()
        .orElse(relationId);
  }

  private String getDisplayTitle(String draftId, Map<String, String> draftTitleById) {
    return Optional.ofNullable(draftTitleById.get(draftId)).orElse(draftId);
  }

  private String getTitleText(SingleConcept concept) {
    if (concept.getTitles() == null || concept.getTitles().isEmpty()) return concept.getId();
    return concept.getTitles().get(0).getText();
  }

  private SingleConcept conceptRef(String conceptId) {
    return new SingleConcept().id(conceptId).entityType(EntityType.SINGLE_CONCEPT);
  }

  private LocalisableText localisableText(String language, String text) {
    return new LocalisableText().lang(language).text(text);
  }

  private String getString(Map<String, Object> map, String camelKey, String snakeKey) {
    Object value = map.get(camelKey);
    if (value == null) value = map.get(snakeKey);
    return value instanceof String stringValue ? stringValue : null;
  }

  private Object normalizeSnakeCaseKeys(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> normalized = new LinkedHashMap<>();
      map.forEach(
          (key, nestedValue) ->
              normalized.put(snakeToLowerCamel(String.valueOf(key)), normalizeSnakeCaseKeys(nestedValue)));
      return normalized;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(this::normalizeSnakeCaseKeys).toList();
    }
    return value;
  }

  private String snakeToLowerCamel(String value) {
    StringBuilder result = new StringBuilder();
    boolean upperNext = false;
    for (char character : value.toCharArray()) {
      if (character == '_') {
        upperNext = true;
      } else if (upperNext) {
        result.append(Character.toUpperCase(character));
        upperNext = false;
      } else {
        result.append(character);
      }
    }
    return result.toString();
  }

  private record QueryExpansionContext(
      String profileName,
      QueryExpansionProfileEntity profile,
      List<QueryExpansionProfileRelationEntity> effectiveRelations,
      QueryExpansionConfig config) {}
}
