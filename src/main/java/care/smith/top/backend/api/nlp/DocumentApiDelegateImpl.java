package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.Document;
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
    public ResponseEntity<List<Document>> getDocuments(List<String> include, String id, Integer page, Integer pageSize) {
        return new ResponseEntity<>(documentService.getDocuments(), HttpStatus.OK);
    }

    public ResponseEntity<List<Document>> getDocumentsByPage(int page, int pageSize) {
        return new ResponseEntity<>(documentService.getDocumentsByPage(page, pageSize), HttpStatus.OK);
    }
}
