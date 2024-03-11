package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptclusterApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.ConceptClusterPage;
import care.smith.top.model.PipelineResponse;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterApiDelegateImpl implements ConceptclusterApiDelegate {
  private final HashMap<String, Thread> conceptClusterProcesses = new HashMap<>();
  @Autowired private ConceptClusterService conceptClusterService;

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClusterByDocumentId(
      String documentId, String dataSource, List<String> include, Integer page) {
    return ConceptclusterApiDelegate.super.getConceptClusterByDocumentId(
        documentId, dataSource, include, page);
  }

  @Override
  public ResponseEntity<PipelineResponse> createConceptClustersForPipelineId(
      String pipelineId, List<String> graphIds) {
    PipelineResponse response = new PipelineResponse().pipelineId(pipelineId);
    conceptClusterService.evictConceptsFromCache();
    if (!conceptClusterProcesses.containsKey(pipelineId)) {
      conceptClusterProcesses.put(
          pipelineId,
          conceptClusterService.createSpecificGraphsInNeo4j(pipelineId, graphIds).getRight());
      conceptClusterService.setPipelineResponseStatus(
          response, "STARTED", "Started Concept Clusters creation ...");
    } else {
      if (conceptClusterProcesses.get(pipelineId).isAlive()) {
        conceptClusterService.setPipelineResponseStatus(
            response, "RUNNING", "Concept Clusters creation is still running ...");
      } else {
        conceptClusterService.setPipelineResponseStatus(
            response, "FINISHED", "Finished Concept Cluster creation for this process.");
      }
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ConceptCluster> getConceptClusterById(
      String conceptId, List<String> include) {
    return ConceptclusterApiDelegate.super.getConceptClusterById(conceptId, include);
  }

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClusters(
      List<String> labelsText,
      List<String> phraseId,
      Boolean recalculateCache,
      Integer page,
      List<String> include) {
    // ToDo: filter by phraseText
    if (Boolean.TRUE.equals(recalculateCache)) conceptClusterService.evictConceptsFromCache();
    return ResponseEntity.ok(ApiModelMapper.toConceptClusterPage(conceptClusterService.concepts()));
  }
}
