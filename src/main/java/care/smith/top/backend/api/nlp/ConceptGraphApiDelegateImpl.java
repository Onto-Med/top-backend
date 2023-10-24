package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptgraphsApiDelegate;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
}
