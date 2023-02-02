package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {

    @Autowired DocumentService documentService;

    @Override
    public ResponseEntity<List<Document>> getDocumentsByConceptIds(List<String> conceptId, Boolean idOnly, String gatheringMode, String name, Boolean exemplarOnly) {
        if (!Objects.equals(gatheringMode, "intersection")) {
            return new ResponseEntity<>(documentService.getDocumentsForConcepts(Set.copyOf(conceptId), idOnly, exemplarOnly), HttpStatus.OK);
        } else {
            Map<String, Document> hashMapDocuments = new HashMap<>();
            List<Set<String>> listOfSets = new ArrayList<>();
            ListIterator<String> it = conceptId.listIterator();

            while (it.hasNext()) {
                int idx = it.nextIndex();
                List<Document> documentList = documentService.getDocumentsForConcepts(Set.of(it.next()), idOnly, exemplarOnly);
                for (Document doc: documentList) {
                    hashMapDocuments.put(doc.getId(), doc);
                }
                listOfSets.add(documentList
                        .stream()
                        .map(Document::getId)
                        .collect(Collectors.toSet())
                );
            }

            return new ResponseEntity<>(
                    listOfSets
                            .stream()
                            .skip(1)
                            .collect( () -> listOfSets.get(0), Set::retainAll, Set::retainAll)
                            .stream()
                            .map(hashMapDocuments::get)
                            .collect(Collectors.toList()),
                    HttpStatus.OK);
        }
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
