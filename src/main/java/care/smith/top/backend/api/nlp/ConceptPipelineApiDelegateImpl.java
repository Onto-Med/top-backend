package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptPipelineApiDelegate;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.model.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConceptPipelineApiDelegateImpl implements ConceptPipelineApiDelegate {
  private final Logger LOGGER = Logger.getLogger(ConceptPipelineApiDelegateImpl.class.getName());

  @Autowired private ConceptGraphsService conceptGraphsService;
  @Autowired private DocumentQueryService documentQueryService;

  @Value("top.documents.default-adapter")
  private String defaultDataSourceId;

  @Override
  public ResponseEntity<Map<String, ConceptGraphStat>> getConceptGraphStatistics(
      String pipelineId) {
    Map<String, ConceptGraphStat> statistics =
        conceptGraphsService.getAllConceptGraphStatistics(pipelineId);
    if (statistics == null) return ResponseEntity.of(Optional.empty());
    return ResponseEntity.ok(statistics);
  }

  @Override
  public ResponseEntity<ConceptGraph> getConceptGraph(String pipelineId, String graphId) {
    return ResponseEntity.ok(
        conceptGraphsService.getConceptGraphForIdAndProcess(graphId, pipelineId));
  }

  @Override
  public ResponseEntity<List<ConceptGraphPipeline>> getConceptGraphPipelines() {
    return ResponseEntity.ok(conceptGraphsService.getAllStoredProcesses());
  }

  @Override
  public ResponseEntity<Void> deleteConceptPipelineById(String pipelineId) {
    //ToDo: all set in the Service
    return ResponseEntity.ok().build();
//    return ConceptPipelineApiDelegate.super.deleteConceptPipelineById(pipelineId);
  }

  @Override
  public ResponseEntity<ConceptGraphPipeline> getConceptGraphPipelineById(String pipelineId) {
    ConceptGraphPipeline pipeline = new ConceptGraphPipeline();
    try {
       pipeline = conceptGraphsService.getAllStoredProcesses().stream()
          .filter(conceptGraphPipeline -> conceptGraphPipeline.getPipelineId().equalsIgnoreCase(pipelineId))
          .findFirst().orElseThrow();
    } catch (NoSuchElementException e) {
      LOGGER.fine(String.format("A pipeline with id '%s' was not found", pipelineId));
    }
    return ResponseEntity.ok(pipeline);
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipelineWithJson(
      ConceptPipelineConfigRequest conceptPipelineConfigRequest) {
    return ConceptPipelineApiDelegate.super.startConceptGraphPipelineWithJson(conceptPipelineConfigRequest);
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
    if (data == null && dataSourceId == null && defaultDataSourceId == null) {
      return ResponseEntity.badRequest()
          .body(
              new PipelineResponse()
                  .pipelineId(pipelineId != null ? pipelineId : "default")
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
      //ToDo: this should go somewhere where it can be more easily adapted later;
      // also index right now in the concept graphs api only supports one value
      documentQueryService
          .getTextAdapterConfig(StringUtils.defaultString(dataSourceId, defaultDataSourceId))
          .ifPresent(textAdapterConfig -> {
            List<String> lines = new ArrayList<>(List.of(
                String.format("\"url\": \"%s\"", textAdapterConfig.getConnection().getUrl()),
                String.format("\"port\": \"%s\"", textAdapterConfig.getConnection().getPort()),
                String.format("\"index\": \"%s\"", Arrays.stream(textAdapterConfig.getIndex()).findFirst().orElseThrow()),
                String.format("\"size\": \"%s\"", textAdapterConfig.getBatchSize())
            ));
            if (textAdapterConfig.getReplaceFields() != null) {
              lines.add(String.format("\"replace_keys\": \"%s\"",
                  textAdapterConfig.getReplaceFields().keySet().stream()
                      .map(key -> key + ": " + textAdapterConfig.getReplaceFields().get(key))
                      .collect(Collectors.joining(", ", "{", "}"))));
            }
            File tempFile = null;
            try {
              tempFile = File.createTempFile("tmp-", "-document_server_config");
              Files.write(tempFile.toPath(), lines);
              tempFile.deleteOnExit();
            } catch (IOException e) {
              LOGGER.severe("Couldn't create temporary file to send to the concept graphs api as a document_server_config.");
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
              pipelineId,
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
}
