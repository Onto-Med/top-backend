package care.smith.top.backend.api.nlp;

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
    if ((clusterResponse.getStatus().equals(PipelineResponseStatus.SUCCESSFUL)
            && graphResponse.getStatus().equals(PipelineResponseStatus.SUCCESSFUL))
        || graphResponse
            .getResponse()
            .contains(String.format("no such process '%s'", finalPipelineId)))
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
    documentQueryService
        .getTextAdapterConfig(
            StringUtils.defaultString(
                !Objects.equals(requestParams.get("name"), "default")
                    ? requestParams.get("name")
                    : null,
                defaultDataSourceId))
        .ifPresent(
            textAdapterConfig -> {
              Map<String, String> configMap = createDocumentServerConfigMap(textAdapterConfig);
              documentServerConfig.set(new JSONObject(configMap));
            });
    request.put("document_server", documentServerConfig.get());

    pipelineResponse =
        conceptGraphsService.initPipeline(
            requestParams.get("name"),
            requestParams.get("language"),
            queryArgs.get("skip_present"),
            queryArgs.get("return_statistics"),
            request);
    if (pipelineResponse.getStatus().equals(PipelineResponseStatus.FAILED)) {
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

    if (data == null) {
      documentQueryService
          .getTextAdapterConfig(StringUtils.defaultString(dataSourceId, defaultDataSourceId))
          .ifPresent(
              textAdapterConfig -> {
                List<String> lines = createDocumentServerConfigLines(textAdapterConfig);
                File tempFile = null;
                try {
                  tempFile = File.createTempFile("tmp-", "-document_server_config");
                  Files.write(tempFile.toPath(), lines);
                  tempFile.deleteOnExit();
                } catch (IOException e) {
                  LOGGER.severe(
                      "Couldn't create temporary file to send to the concept graphs api as a document_server_config.");
                }
                configMap.put("document_server", tempFile);
              });
    }

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
      if (pipelineResponse.getStatus().equals(PipelineResponseStatus.FAILED)) {
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

  //ToDo: would be better if this conversion would be done via the concept graphs api to be in accordance with every change to the pipeline name that's done there
  private String stringConformity(String s) {
    if (s == null || s.isEmpty()) return "default";
    return s.toLowerCase().replaceAll("\\s+", "_");
  }
}

