package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptPipelineApiDelegate;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.model.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConceptGraphApiDelegateImpl implements ConceptPipelineApiDelegate {
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
  public ResponseEntity<List<ConceptGraphPipeline>> getConceptPipelines() {
    return ResponseEntity.ok(conceptGraphsService.getAllStoredProcesses());
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

    if (data == null) {
      documentQueryService
          .getTextAdapterConfigPath(StringUtils.defaultString(dataSourceId, defaultDataSourceId))
          .ifPresent(p -> configMap.put("document_Server", p.toFile()));
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
