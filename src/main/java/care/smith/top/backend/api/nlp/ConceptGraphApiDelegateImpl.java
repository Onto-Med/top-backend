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
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConceptGraphApiDelegateImpl implements ConceptgraphsApiDelegate {
  private final ConceptGraphsService conceptGraphsService;
  private final ConceptClusterService conceptClusterService;

  public ConceptGraphApiDelegateImpl(
      ConceptGraphsService conceptGraphsService, ConceptClusterService conceptClusterService) {
    this.conceptGraphsService = conceptGraphsService;
    this.conceptClusterService = conceptClusterService;
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
      MultipartFile dataServerConfig) {
    if (data == null && (dataServerConfig == null && conceptGraphsService.getDocumentServerAddress() == null)) {
      return ResponseEntity.badRequest().body(
          new PipelineResponse()
              .name(process != null ? process : "default")
              .response("Neither 'data' nor configuration for a data server ('dataServerConfig') were provided. One of either is needed.")
      );
    }
    Map<String, File> configMap =
        Map.of(
                "data", dataConfig,
                "embedding", embeddingConfig,
                "clustering", clusteringConfig,
                "graph", graphConfig)
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
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
    if (dataServerConfig != null) {
      try {
        configMap.put("data_server", dataServerConfig.getResource().getFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else if (data != null && conceptGraphsService.getDocumentServerAddress() != null) {
      //ToDo: create File of data server specs from conceptGraphsService value'd fields and put it in configMap
    }

    try {
      PipelineResponse pipelineResponse;
      if (data != null) {
        pipelineResponse = conceptGraphsService.initPipelineWithDataUploadAndWithConfigs(
                data.getResource().getFile(),
                labels != null ? labels.getResource().getFile() : null,
                process,
                lang,
                skipPresent,
                returnStatistics,
                configMap);
      } else {
        pipelineResponse = conceptGraphsService.initPipelineWithDataServerAndWithConfigs(
                labels != null ? labels.getResource().getFile() : null,
                process,
                lang,
                skipPresent,
                returnStatistics,
                configMap);
      }
      if (pipelineResponse.getStatus().equals(PipelineResponseStatus.FAILED)) {
        return ResponseEntity.of(Optional.of(pipelineResponse));
      }
      return ResponseEntity.ok(pipelineResponse);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
