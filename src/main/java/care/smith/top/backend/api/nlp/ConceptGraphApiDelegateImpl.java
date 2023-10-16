package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptgraphsApiDelegate;
import care.smith.top.model.ConceptGraphStat;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ConceptGraphApiDelegateImpl implements ConceptgraphsApiDelegate {
  @Override
  public ResponseEntity<List<ConceptGraphStat>> getConceptGraphStatistics(List<String> include, String process) {
    return ConceptgraphsApiDelegate.super.getConceptGraphStatistics(include, process);
  }
}
