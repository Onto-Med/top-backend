package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptclusterApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.ConceptClusterPage;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterApiDelegateImpl implements ConceptclusterApiDelegate {

  private final ConceptClusterService conceptClusterService;

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
  public ResponseEntity<List<ConceptCluster>> getConceptClusters(String phraseText) {
    // ToDo: filter by phraseText
    return ResponseEntity.ok(conceptClusterService.concepts(true));
  }
}
