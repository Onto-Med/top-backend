package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.service.nlp.ConceptService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.model.Document;
import care.smith.top.model.Phrase;
import care.smith.top.model.DocumentPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {

    @Autowired DocumentService documentService;
    @Autowired PhraseService phraseService;
    @Autowired ConceptService conceptService;

    @Override
    public ResponseEntity<List<Document>> getDocumentIdsByConceptIds(List<String> conceptId, String gatheringMode, String name, Boolean exemplarOnly) {
        //ToDo: filter by 'name' not implemented
        if (!Objects.equals(gatheringMode, "intersection")) {
            return new ResponseEntity<>(documentService.getDocumentsForConcepts(Set.copyOf(conceptId), exemplarOnly), HttpStatus.OK);
        } else {
            Map<String, Document> hashMapDocuments = new HashMap<>();
            List<Set<String>> listOfSets = new ArrayList<>();
            ListIterator<String> it = conceptId.listIterator();

            while (it.hasNext()) {
                int idx = it.nextIndex();
                List<Document> documentList = documentService.getDocumentsForConcepts(Set.of(it.next()), exemplarOnly);
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
    public ResponseEntity<Document> getDocumentById(String documentId, List<String> conceptIds, List<String> include) {
        // Todo: just testing
        if (conceptIds != null) {
            String[] conceptPhrases = conceptIds.stream()
                    .map(cid -> phraseService.getPhrasesForConcept(cid).stream()
                            .map(Phrase::getText)
                            .collect(Collectors.joining("|")))
                    .collect(Collectors.joining("|")).split("\\|");

            String[] shouldTerms = phraseService.getPhrasesForDocument(documentId, false)
                    .stream()
                    .map(Phrase::getText)
                    .filter(s -> Arrays.asList(conceptPhrases).contains(s))
//                            .filter(s -> s.matches("[a-zA-Z]+"))
                    .toArray(String[]::new);

            List<Document> documents = documentService.getDocumentsByPhrases(shouldTerms, new String[]{"text"});

            Optional<Document> document = documents.stream().filter(d -> d.getId().equals(documentId)).findFirst();
            return document.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(documentService.getDocumentById(documentId), HttpStatus.OK));
        }
        return new ResponseEntity<>(documentService.getDocumentById(documentId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DocumentPage> getDocumentsByPhraseIds(String phraseId, List<String> include, String name, Integer page) {
        return DocumentApiDelegate.super.getDocumentsByPhraseIds(phraseId, include, name, page);
    }

    @Override
    public ResponseEntity<List<Document>> getDocuments(List<String> include, List<String> phraseText) {
        return new ResponseEntity<>(documentService.getDocumentsByTerms(phraseText.toArray(new String[0]), new String[]{"text"}), HttpStatus.OK);
    }
}
