package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.RagApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.service.nlp.RAGService;
import care.smith.top.model.RAGAnswer;
import care.smith.top.top_document_query.adapter.TextAdapter;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

import java.util.logging.Logger;

public class RAGApiDelegateImpl implements RagApiDelegate {
    private final Logger LOGGER = Logger.getLogger(RAGApiDelegateImpl.class.getName());
    private final RAGService ragService;
    private final DocumentService documentService;

    public RAGApiDelegateImpl(RAGService ragService, DocumentService documentService) {
        this.ragService = ragService;
        this.documentService = documentService;
    }

    @Override
    public ResponseEntity<String> initializeRAG(String process, Boolean force, Object body) {
        return ResponseEntity.ok(ragService.initializeRAG(process, force, (JSONObject) body));
    }

    @Override
    public ResponseEntity<RAGAnswer> poseQuestionToRAG(String process, String q) {
        return ResponseEntity.ok(ragService.poseQuestion(process, q));
    }

    private TextAdapter getTextAdapter(String dataSource) {
        try {
            return documentService.getAdapterForDataSource(dataSource);
        } catch (InstantiationException e) {
            LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
            return null;
        }
    }
}