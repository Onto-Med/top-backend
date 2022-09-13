package care.smith.top.backend.neo4j_nlp.resource.api;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.model.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {
    @Override
    public ResponseEntity<List<Document>> getDocuments(String id) {
        return DocumentApiDelegate.super.getDocuments(id);
    }
}
