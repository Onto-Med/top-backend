package care.smith.top.backend.service.nlp;

import care.smith.top.top_document_query.concept_graphs_api.QueryExpansionManager;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileEntity;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QueryExpansionService {
  private final QueryExpansionManager queryExpansionManager;
  private final boolean cgApiEnabled;
  private final String llmModel;
  private final String llmProvider;
  private final String llmBaseUrl;
  private final String llmApiKey;
  private final boolean llmNoStructuredOutput;

  public QueryExpansionService(
      @Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri,
      @Value("${top.documents.concept-graphs-api.enabled}") boolean cgApiEnabled,
      @Value("${top.documents.query-expansion.llm.model:}") String llmModel,
      @Value("${top.documents.query-expansion.llm.provider:}") String llmProvider,
      @Value("${top.documents.query-expansion.llm.base-url:}") String llmBaseUrl,
      @Value("${top.documents.query-expansion.llm.api-key:}") String llmApiKey,
      @Value("${top.documents.query-expansion.llm.no-structured-output:false}")
          boolean llmNoStructuredOutput) {
    this.cgApiEnabled = cgApiEnabled;
    this.llmModel = llmModel;
    this.llmProvider = llmProvider;
    this.llmBaseUrl = llmBaseUrl;
    this.llmApiKey = llmApiKey;
    this.llmNoStructuredOutput = llmNoStructuredOutput;
    QueryExpansionManager tmpManager;
    try {
      tmpManager = new QueryExpansionManager(conceptGraphsApiUri);
    } catch (MalformedURLException | URISyntaxException e) {
      try {
        tmpManager = new QueryExpansionManager("http://localhost:9010");
      } catch (MalformedURLException | URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
    }
    queryExpansionManager = tmpManager;
  }

  public Optional<List<QueryExpansionProfileEntity>> getProfiles() {
    if (!cgApiEnabled) return Optional.empty();
    return queryExpansionManager.getProfiles();
  }

  public Optional<QueryExpansionProfileEntity> getProfile(String profileName) {
    if (!cgApiEnabled) return Optional.empty();
    return queryExpansionManager.getProfile(profileName);
  }

  public Optional<Map<String, Object>> expand(
      Map<String, Object> request,
      String profile,
      List<String> allowedRelations,
      List<Map<String, Object>> relationDefinitions) {
    if (!cgApiEnabled) return Optional.empty();

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("term", request.get("term"));
    Object language = request.get("language");
    payload.put("language", language == null ? "en" : language);
    Object categories = request.get("categories");
    if (categories instanceof List<?> categoryList && !categoryList.isEmpty()) {
      payload.put("categories", categoryList);
    }
    Object limitPerCategory = request.get("limitPerCategory");
    if (limitPerCategory == null) limitPerCategory = request.get("limit_per_category");
    if (limitPerCategory != null) {
      payload.put("limit_per_category", limitPerCategory);
    }
    payload.put("relations", allowedRelations);
    payload.put("relation_definitions", relationDefinitions);
    payload.put("prompt", Map.of("profile", profile));
    payload.put("grounding", Map.of("include_llm_only", true));
    payload.put("llm", getLlmConfig());

    return queryExpansionManager.expand(payload);
  }

  private Map<String, Object> getLlmConfig() {
    Map<String, Object> options = new LinkedHashMap<>();
    putIfConfigured(options, "provider", llmProvider);
    putIfConfigured(options, "base_url", llmBaseUrl);
    putIfConfigured(options, "api_key", llmApiKey);
    if (llmNoStructuredOutput) options.put("no_structured_output", true);

    Map<String, Object> llm = new LinkedHashMap<>();
    llm.put("model", llmModel);
    if (!options.isEmpty()) llm.put("options", options);
    return llm;
  }

  private void putIfConfigured(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) target.put(key, value);
  }
}
