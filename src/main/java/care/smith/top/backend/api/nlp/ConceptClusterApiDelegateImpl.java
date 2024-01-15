package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptclusterApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.ConceptClusterPage;
import care.smith.top.model.PipelineResponse;
import java.util.HashMap;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterApiDelegateImpl implements ConceptclusterApiDelegate {

  private final ConceptClusterService conceptClusterService;
  private HashMap<String, Thread> conceptClusterProcesses = new HashMap<>();

  public ConceptClusterApiDelegateImpl(ConceptClusterService conceptClusterService) {
    this.conceptClusterService = conceptClusterService;
  }

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClustersByDocumentId(
      String documentId, List<String> include, String name, Integer page) {
    return ConceptclusterApiDelegate.super.getConceptClustersByDocumentId(
        documentId, include, name, page);
  }

  @Override
  public ResponseEntity<PipelineResponse> createConceptClustersForProcessId(
      String processId, List<String> include, List<String> graphIds) {
    PipelineResponse response = new PipelineResponse().name(processId);
    conceptClusterService.evictConceptsFromCache();
    if (!conceptClusterProcesses.containsKey(processId)) {
      conceptClusterProcesses.put(
          processId,
          conceptClusterService.createSpecificGraphsInNeo4j(processId, graphIds).getRight());
      conceptClusterService.setPipelineResponseStatus(
          response, "STARTED", "Started Concept Clusters creation ...");
    } else {
      if (conceptClusterProcesses.get(processId).isAlive()) {
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
  public ResponseEntity<ConceptClusterPage> getConceptClustersByPhraseId(
      String phraseId, List<String> include, String name, Integer page) {
    return ConceptclusterApiDelegate.super.getConceptClustersByPhraseId(
        phraseId, include, name, page);
  }

  @Override
  public ResponseEntity<List<ConceptCluster>> getConceptClusters(
      String phraseText, Boolean recalculateCache) {
    // ToDo: filter by phraseText
    if (Boolean.TRUE.equals(recalculateCache)) conceptClusterService.evictConceptsFromCache();
    return ResponseEntity.ok(conceptClusterService.concepts());
  }
}
