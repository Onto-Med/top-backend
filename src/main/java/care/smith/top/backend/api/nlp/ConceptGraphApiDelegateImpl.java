package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptgraphsApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.model.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConceptGraphApiDelegateImpl implements ConceptgraphsApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(ConceptGraphApiDelegateImpl.class.getName());
  private final ConceptGraphsService conceptGraphsService;

  public ConceptGraphApiDelegateImpl(
      ConceptGraphsService conceptGraphsService, ConceptClusterService conceptClusterService) {
    this.conceptGraphsService = conceptGraphsService;
  }

  @Override
  public ResponseEntity<Map<String, ConceptGraphStat>> getConceptGraphStatistics(
      List<String> include, String process) {
    Map<String, ConceptGraphStat> statistics =
        conceptGraphsService.getAllConceptGraphStatistics(process);
    if (statistics == null) return ResponseEntity.of(Optional.empty());
    return ResponseEntity.ok(statistics);
  }

  @Override
  public ResponseEntity<ConceptGraph> getConceptGraph(
      String processId, String graphId, List<String> include) {
    return ResponseEntity.ok(
        conceptGraphsService.getConceptGraphForIdAndProcess(graphId, processId));
  }

  @Override
  public ResponseEntity<List<ConceptGraphProcess>> getStoredProcesses(List<String> include) {
    return ResponseEntity.ok(conceptGraphsService.getAllStoredProcesses());
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipelineWithoutUpload(
      String process,
      List<String> include,
      String lang,
      Boolean skipPresent,
      Boolean returnStatistics) {
    return startConceptGraphPipeline(
        process,
        include,
        lang,
        skipPresent,
        returnStatistics,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipeline(
      String process,
      List<String> include,
      String lang,
      Boolean skipPresent,
      Boolean returnStatistics,
      MultipartFile data,
      MultipartFile labels,
      MultipartFile dataConfig,
      MultipartFile embeddingConfig,
      MultipartFile clusteringConfig,
      MultipartFile graphConfig,
      MultipartFile documentServerConfig) {
    if (data == null && documentServerConfig == null) {
      return ResponseEntity.badRequest()
          .body(
              new PipelineResponse()
                  .name(process != null ? process : "default")
                  .response(
                      "Neither 'data' nor configuration for a document server ('documentServerConfig') were provided. "
                          + "There also seems no default document server to be available. One of either is needed.")
                  .status(PipelineResponseStatus.FAILED));
    }
    Map<String, File> configMap =
        Stream.of(
                Pair.of("data", dataConfig),
                Pair.of("embedding", embeddingConfig),
                Pair.of("clustering", clusteringConfig),
                Pair.of("graph", graphConfig))
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

    if (documentServerConfig != null) {
      try {
        configMap.put("document_server", documentServerConfig.getResource().getFile());
      } catch (IOException e) {
        LOGGER.severe(
            "Couldn't access document_server_config file. Something went wrong with the upload.");
        throw new RuntimeException(e);
      }
    }

    try {
      PipelineResponse pipelineResponse =
          conceptGraphsService.initPipeline(
              data != null ? data.getResource().getFile() : null,
              labels != null ? labels.getResource().getFile() : null,
              configMap,
              process,
              lang,
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
