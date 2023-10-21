package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptgraphsApiDelegate;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConceptGraphApiDelegateImpl implements ConceptgraphsApiDelegate {
  private final ConceptGraphsService conceptGraphsService;

  public ConceptGraphApiDelegateImpl(ConceptGraphsService conceptGraphsService) {
    this.conceptGraphsService = conceptGraphsService;
  }

  @Override
  public ResponseEntity<List<ConceptGraphStat>> getConceptGraphStatistics(List<String> include, String process) {
    return ConceptgraphsApiDelegate.super.getConceptGraphStatistics(include, process);
  }

  @Override
  public ResponseEntity<ConceptGraph> getConceptGraph(List<String> include, String processId, String graphId) {
    return ConceptgraphsApiDelegate.super.getConceptGraph(include, processId, graphId);
  }

  @Override
  public ResponseEntity<List<ConceptGraphProcess>> getStoredProcesses(List<String> include) {
    conceptGraphsService.getAllStoredProcesses();
    return ConceptgraphsApiDelegate.super.getStoredProcesses(include);
  }
}
