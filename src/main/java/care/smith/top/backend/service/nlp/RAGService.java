package care.smith.top.backend.service.nlp;

import care.smith.top.model.RAGAnswer;
import care.smith.top.top_document_query.concept_graphs_api.ConceptPipelineManager;
import care.smith.top.top_document_query.concept_graphs_api.RAGManager;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

@Service
public class RAGService {
    private final RAGManager ragManager;

    public RAGService(@Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri) {
        RAGManager tmpManager;
        try {
            tmpManager = new RAGManager(conceptGraphsApiUri);
        } catch (MalformedURLException e) {
            try {
                tmpManager = new RAGManager("http://localhost:9010");
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
        ragManager = tmpManager;
    }

    public String initializeRAG(String process, boolean force, JSONObject jsonBody) {
        return ragManager.initRag(process, force, jsonBody);
    }

    public RAGAnswer poseQuestion(String process, String question) {
        return ragManager.poseQuestion(process, question);
    }
}
