package care.smith.top.backend.api.nlp;

import static care.smith.top.backend.util.nlp.NLPUtils.stringConformity;

import care.smith.top.backend.api.ConceptPipelineApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConceptPipelineApiDelegateImpl implements ConceptPipelineApiDelegate {
  private final Logger LOGGER = Logger.getLogger(ConceptPipelineApiDelegateImpl.class.getName());

  @Autowired private ConceptGraphsService conceptGraphsService;
  @Autowired private ConceptClusterService conceptClusterService;
  @Autowired private DocumentQueryService documentQueryService;

  @Value("top.documents.default-adapter")
  private String defaultDataSourceId;

  @Override
  public ResponseEntity<Map<String, ConceptGraphStat>> getConceptGraphStatistics(
      String pipelineId) {
    Map<String, ConceptGraphStat> statistics =
        conceptGraphsService.getAllConceptGraphStatistics(stringConformity(pipelineId));
    if (statistics == null) return ResponseEntity.of(Optional.empty());
    return ResponseEntity.ok(statistics);
  }

  @Override
  public ResponseEntity<ConceptGraph> getConceptGraph(String pipelineId, String graphId) {
    return ResponseEntity.ok(
        conceptGraphsService.getConceptGraphForIdAndProcess(graphId, stringConformity(pipelineId)));
  }

  @Override
  public ResponseEntity<List<ConceptGraphPipeline>> getConceptGraphPipelines() {
    return ResponseEntity.ok(conceptGraphsService.getAllStoredProcesses());
  }

  @Override
  public ResponseEntity<Void> deleteConceptPipelineById(String pipelineId) {
    final String finalPipelineId = stringConformity(pipelineId);
    PipelineResponse clusterResponse =
        conceptClusterService.deleteCompletePipelineAndResults(finalPipelineId);
    PipelineResponse graphResponse = conceptGraphsService.deletePipeline(finalPipelineId);
    if ((Objects.equals(clusterResponse.getStatus(), PipelineResponseStatus.SUCCESSFUL)
            && Objects.equals(graphResponse.getStatus(), PipelineResponseStatus.SUCCESSFUL))
        || Objects.requireNonNull(graphResponse.getResponse()).contains("no such process"))
      return ResponseEntity.ok().build();
    return ResponseEntity.internalServerError().build();
  }

  @Override
  public ResponseEntity<ConceptGraphPipeline> getConceptGraphPipelineById(String pipelineId) {
    ConceptGraphPipeline pipeline = new ConceptGraphPipeline();
    final String finalPipelineId = stringConformity(pipelineId);
    try {
      pipeline =
          conceptGraphsService.getAllStoredProcesses().stream()
              .filter(
                  conceptGraphPipeline ->
                      conceptGraphPipeline.getPipelineId().equalsIgnoreCase(finalPipelineId))
              .findFirst()
              .orElseThrow();
    } catch (NoSuchElementException e) {
      LOGGER.fine(String.format("A pipeline with id '%s' was not found", finalPipelineId));
    }
    return ResponseEntity.ok(pipeline);
  }

  @Override
  public ResponseEntity<String> getConceptGraphPipelineConfiguration(
      String pipelineId, String language) {
    String config = conceptGraphsService.getPipelineConfig(stringConformity(pipelineId), language);
    if (Objects.equals(config, "{}")) return ResponseEntity.notFound().build();
    JSONObject jsonObject = new JSONObject(config);
    String configStr = jsonObject.has("config") ? jsonObject.get("config").toString() : "{}";
    return ResponseEntity.of(Optional.ofNullable(configStr));
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipelineWithJson(
      String conceptPipelineConfigRequest) {
    PipelineResponse pipelineResponse;
    // ToDo: Attention! skip_present & return_statistics need to be put into the json as well (so
    // it's not the same json as from concept-graphs-api)
    JSONObject request = new JSONObject(conceptPipelineConfigRequest.replace("\\", ""));
    HashMap<String, String> requestParams =
        new HashMap<>(
            Map.of(
                "name", "default",
                "language", "en"));
    HashMap<String, Boolean> queryArgs =
        new HashMap<>(
            Map.of(
                "skip_present", true,
                "return_statistics", false));
    List<String> keyList = List.of("name", "language", "skip_present", "return_statistics");
    for (String key : keyList) {
      key = key.toLowerCase();
      if (!request.has(key)) continue;
      if (List.of("name", "language").contains(key)) {
        requestParams.put(key, request.getString(key));
      } else if (List.of("skip_present", "return_statistics").contains(key)) {
        queryArgs.put(key, request.getBoolean(key));
        request.remove(key);
      }
    }

    AtomicReference<JSONObject> documentServerConfig = new AtomicReference<>();
    AtomicReference<JSONObject> vectorStoreServerConfig = new AtomicReference<>();
    AtomicReference<JSONObject> cgApiConfig = new AtomicReference<>();
    documentQueryService
        .getTextAdapterConfig(
            StringUtils.defaultString(
                !Objects.equals(requestParams.get("name"), "default")
                    ? requestParams.get("name")
                    : null,
                defaultDataSourceId))
        .ifPresent(
            textAdapterConfig -> {
              documentServerConfig.set(
                  new JSONObject(createDocumentServerConfigMap(textAdapterConfig)));
              vectorStoreServerConfig.set(
                  new JSONObject(createVectorStoreServerMap(textAdapterConfig)));
              cgApiConfig.set(new JSONObject(createCgServerMap(textAdapterConfig)));
            });
    request.put("document_server", documentServerConfig.get());
    request.put("vectorstore_server", vectorStoreServerConfig.get());

    pipelineResponse =
        conceptGraphsService.initPipeline(
            requestParams.get("name"),
            requestParams.get("language"),
            queryArgs.get("skip_present"),
            queryArgs.get("return_statistics"),
            request,
            cgApiConfig.get());
    if (Objects.requireNonNull(pipelineResponse.getStatus())
        .equals(PipelineResponseStatus.FAILED)) {
      return ResponseEntity.of(Optional.of(pipelineResponse));
    }
    return ResponseEntity.ok(pipelineResponse);
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipeline(
      String pipelineId,
      String dataSourceId,
      Boolean skipPresent,
      Boolean returnStatistics,
      String language,
      MultipartFile data,
      MultipartFile labels,
      MultipartFile dataConfig,
      MultipartFile embeddingConfig,
      MultipartFile clusteringConfig,
      MultipartFile graphConfig) {
    String finalPipelineId = stringConformity(pipelineId);
    if (data == null && dataSourceId == null && defaultDataSourceId == null) {
      return ResponseEntity.badRequest()
          .body(
              new PipelineResponse()
                  .pipelineId(finalPipelineId)
                  .response(
                      "Neither 'data' nor configuration for a document server ('dataSourceId') were provided. "
                          + "There also seems no default document server to be available. One of either is needed.")
                  .status(PipelineResponseStatus.FAILED));
    }
    Map<String, File> configMap =
        Stream.of(
                Pair.of(ConceptGraphPipelineStepsEnum.DATA.getValue(), dataConfig),
                Pair.of(ConceptGraphPipelineStepsEnum.EMBEDDING.getValue(), embeddingConfig),
                Pair.of(ConceptGraphPipelineStepsEnum.CLUSTERING.getValue(), clusteringConfig),
                Pair.of(ConceptGraphPipelineStepsEnum.GRAPH.getValue(), graphConfig))
            .filter(pair -> pair.getRight() != null)
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      try {
                        return e.getValue().getResource().getFile();
                      } catch (IOException ex) {
                        throw new RuntimeException(ex);
                      }
                    }));

    documentQueryService
        .getTextAdapterConfig(StringUtils.defaultString(dataSourceId, defaultDataSourceId))
        .ifPresent(
            textAdapterConfig -> {
              List<String> lines = createDocumentServerConfigLines(textAdapterConfig);
              File tempFile = null;
              // If data is not provided via text files a document server will be used
              if (data == null) {
                try {
                  tempFile = File.createTempFile("tmp-", "-document_server_config");
                  Files.write(tempFile.toPath(), lines);
                  tempFile.deleteOnExit();
                } catch (IOException e) {
                  LOGGER.severe(
                          "Couldn't create temporary file to send to the concept graphs api as a document_server_config.");
                }
                configMap.put("document_server", tempFile);
              }
              // read and store infos for the vector store server
              try {
                File tempFileVectorStore = File.createTempFile("tmp-", "-vector_store_server_config");
                Files.write(tempFileVectorStore.toPath(), createVectorStoreServerConfigLines());
                tempFileVectorStore.deleteOnExit();
                configMap.put("vectorstore_server", tempFileVectorStore);
              } catch (IOException e) {
                LOGGER.severe(
                        "Couldn't create temporary file to send to the concept graphs api as a vector_store_server_config.");
              }
            });

    try {
      PipelineResponse pipelineResponse =
          conceptGraphsService.initPipeline(
              data != null ? data.getResource().getFile() : null,
              labels != null ? labels.getResource().getFile() : null,
              configMap,
              finalPipelineId,
              language,
              skipPresent,
              returnStatistics);
      if (Objects.equals(pipelineResponse.getStatus(), PipelineResponseStatus.FAILED)) {
        return ResponseEntity.of(Optional.of(pipelineResponse));
      }
      return ResponseEntity.ok(pipelineResponse);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ResponseEntity<Void> stopConceptGraphPipeline(String pipelineId) {
    conceptGraphsService.stopPipeline(stringConformity(pipelineId));
    return ResponseEntity.ok().build();
  }

  private Map<String, String> createVectorStoreServerMap(TextAdapterConfig textAdapterConfig) {
    Map<String, String> configMap =
        new HashMap<>(
            Map.of(
                "url", textAdapterConfig.getVectorStore().getConnection().getUrl(),
                "port", textAdapterConfig.getVectorStore().getConnection().getPort()));
    if (textAdapterConfig.getVectorStore().getConnection().getAlternateUrl() != null) {
      configMap.put(
          "alternate_url", textAdapterConfig.getVectorStore().getConnection().getAlternateUrl());
    }
    return configMap;
  }

  private Map<String, String> createCgServerMap(TextAdapterConfig textAdapterConfig) {
    Map<String, String> configMap =
        new HashMap<>(
            Map.of(
                "url", textAdapterConfig.getConceptGraph().getConnection().getUrl(),
                "port", textAdapterConfig.getConceptGraph().getConnection().getPort()));
    if (textAdapterConfig.getConceptGraph().getConnection().getAlternateUrl() != null) {
      configMap.put(
          "alternate_url", textAdapterConfig.getConceptGraph().getConnection().getAlternateUrl());
    }
    return configMap;
  }

  private Map<String, String> createDocumentServerConfigMap(TextAdapterConfig textAdapterConfig) {
    // ToDO: index right now in the concept graphs api only supports one value
    Map<String, String> configMap =
        new HashMap<>(
            Map.of(
                "url", textAdapterConfig.getConnection().getUrl(),
                "port", textAdapterConfig.getConnection().getPort(),
                "index", Arrays.stream(textAdapterConfig.getIndex()).findFirst().orElseThrow(),
                "size", String.valueOf(textAdapterConfig.getBatchSize()),
                "label_key", textAdapterConfig.getLabelKey(),
                "other_id", textAdapterConfig.getOtherId()));
    if (textAdapterConfig.getConnection().getAlternateUrl() != null) {
      configMap.put("alternate_url", textAdapterConfig.getConnection().getAlternateUrl());
    }
    if (textAdapterConfig.getReplaceFields() != null) {
      configMap.put(
          "replace_keys",
          textAdapterConfig.getReplaceFields().keySet().stream()
              .map(key -> key + ": " + textAdapterConfig.getReplaceFields().get(key))
              .collect(Collectors.joining(", ", "{", "}")));
    }
    return configMap;
  }

  private List<String> createDocumentServerConfigLines(TextAdapterConfig textAdapterConfig) {
    List<String> l = new ArrayList<>();
    createDocumentServerConfigMap(textAdapterConfig)
        .forEach((k, v) -> l.add(String.format("\"%s\": \"%s\"", k, v)));
    return l;
  }

  private List<String> createVectorStoreServerConfigLines() {
    return List.of("\"url\": \"http://localhost\"", "\"port\": \"8882\"");
  }
}
