package care.smith.top.backend.service.nlp;

import care.smith.top.model.RAGAnswer;
import care.smith.top.model.RAGStatus;
import care.smith.top.top_document_query.concept_graphs_api.RAGManager;
import care.smith.top.top_document_query.util.NLPUtils;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RAGService {
  private final RAGManager ragManager;

  public RAGService(@Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri) {
    RAGManager tmpManager;
    try {
      tmpManager = new RAGManager(conceptGraphsApiUri);
    } catch (MalformedURLException | URISyntaxException e) {
      try {
        tmpManager = new RAGManager("http://localhost:9010");
      } catch (MalformedURLException | URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
    }
    ragManager = tmpManager;
  }

  public String initializeRAG(String process, boolean force, JSONObject jsonBody) {
    String finalProcess = NLPUtils.stringConformity(process);
    return ragManager.initRag(finalProcess, force, jsonBody);
  }

  public RAGAnswer poseQuestion(String process, String question) {
    String finalProcess = NLPUtils.stringConformity(process);
    return ragManager.poseQuestion(finalProcess, question);
  }

  public RAGAnswer poseQuestionWithFilter(String process, String question, String[] docIds) {
    String finalProcess = NLPUtils.stringConformity(process);
    return ragManager.poseQuestion(finalProcess, question, docIds);
  }

  public RAGStatus getStatus(String process) {
    String finalProcess = NLPUtils.stringConformity(process);
    return ragManager.getRAGStatus(finalProcess);
  }

  public boolean isActive(String process) {
    String finalProcess = NLPUtils.stringConformity(process);
    RAGStatus status = getStatus(finalProcess);
    if (status == null) return false;
    return Boolean.TRUE.equals(status.isActive());
  }
}
