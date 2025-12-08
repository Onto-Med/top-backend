package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.RagApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.service.nlp.RAGService;
import care.smith.top.model.RAGAnswer;
import care.smith.top.model.RAGFilter;
import care.smith.top.model.RAGStatus;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class RAGApiDelegateImpl implements RagApiDelegate {
  private final RAGService ragService;

  public RAGApiDelegateImpl(RAGService ragService, DocumentService documentService) {
    this.ragService = ragService;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<String> initializeRAG(String process, Boolean force, Object body) {
    return ResponseEntity.ok(ragService.initializeRAG(process, force, (JSONObject) body));
  }

  @Override
  public ResponseEntity<RAGAnswer> poseQuestionToRAG(String process, String q) {
    return ResponseEntity.ok(ragService.poseQuestion(process, q));
  }

  @Override
  public ResponseEntity<RAGAnswer> poseQuestionToRAGWithFilter(
      String process, String q, RAGFilter ragFilter) {
    return ResponseEntity.ok(
        ragService.poseQuestionWithFilter(
            process,
            q,
            ragFilter.getDocIds() != null
                ? ragFilter.getDocIds().toArray(new String[0])
                : new String[0]));
  }

  @Override
  public ResponseEntity<RAGStatus> getStatusOfRAG(String process) {
    return ResponseEntity.ok(ragService.getStatus(process));
  }
}
