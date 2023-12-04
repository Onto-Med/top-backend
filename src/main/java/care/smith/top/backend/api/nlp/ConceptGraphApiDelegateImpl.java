package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptgraphsApiDelegate;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import care.smith.top.model.PipelineResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class ConceptGraphApiDelegateImpl implements ConceptgraphsApiDelegate {
  private final ConceptGraphsService conceptGraphsService;

  public ConceptGraphApiDelegateImpl(ConceptGraphsService conceptGraphsService) {
    this.conceptGraphsService = conceptGraphsService;
  }

  @Override
  public ResponseEntity<Map<String, ConceptGraphStat>> getConceptGraphStatistics(List<String> include, String process) {
    return ResponseEntity.ok(conceptGraphsService.getAllConceptGraphStatistics(process));
  }

  @Override
  public ResponseEntity<ConceptGraph> getConceptGraph(List<String> include, String processId, String graphId) {
    return ResponseEntity.ok(
        conceptGraphsService.getConceptGraphForIdAndProcess(graphId, processId));
  }

  @Override
  public ResponseEntity<List<ConceptGraphProcess>> getStoredProcesses(List<String> include) {
    return ResponseEntity.ok(conceptGraphsService.getAllStoredProcesses());
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipeline (
      String process, MultipartFile data,
      List<String> include,
      String lang, Boolean skipPresent, Boolean returnStatistics,
      MultipartFile labels,
      MultipartFile dataConfig, MultipartFile embeddingConfig,
      MultipartFile clusteringConfig, MultipartFile graphConfig
  ) {
    Map<String, File> configMap = Map.of(
        "data", dataConfig,
        "embedding", embeddingConfig,
        "clustering", clusteringConfig,
        "graph", graphConfig)
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, e -> {
              try {
                return e.getValue().getResource().getFile();
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            }));

    try {
      return ResponseEntity.ok(
          conceptGraphsService.initPipelineWithConfigs(
              data.getResource().getFile(),
              labels != null ? labels.getResource().getFile(): null,
              process, lang,
              skipPresent, returnStatistics, configMap
          )
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
