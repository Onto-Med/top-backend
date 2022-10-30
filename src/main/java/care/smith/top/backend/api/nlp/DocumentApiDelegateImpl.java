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

    @Autowired DocumentService documentService;

    @Override
    public ResponseEntity<List<Document>> getDocumentByConceptId(String conceptId, Boolean idOnly, List<String> include, String name, Integer page) {
        return new ResponseEntity<>(documentService.getDocumentsForConcept(conceptId, idOnly), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Document> getDocumentById(String documentId, List<String> include) {
        return new ResponseEntity<>(documentService.getDocumentById(documentId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Document>> getDocumentByPhraseId(String phraseId, List<String> include, String name, Integer page) {
        return DocumentApiDelegate.super.getDocumentByPhraseId(phraseId, include, name, page);
    }

    @Override
    public ResponseEntity<List<Document>> getDocuments(List<String> include, String phraseText, String conceptText, String phraseId, String conceptId) {
        return DocumentApiDelegate.super.getDocuments(include, phraseText, conceptText, phraseId, conceptId);
    }
}
