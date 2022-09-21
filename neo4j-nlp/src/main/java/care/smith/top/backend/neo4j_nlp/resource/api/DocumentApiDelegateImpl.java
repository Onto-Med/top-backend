package care.smith.top.backend.neo4j_nlp.resource.api;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.model.Document;
import care.smith.top.backend.neo4j_nlp.resource.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {
    @Autowired
    DocumentService documentService;

    @Override
    public ResponseEntity<List<Document>> getDocuments(String id) {
        return new ResponseEntity<>(
                documentService.getDocuments(), HttpStatus.OK);
    }
}
