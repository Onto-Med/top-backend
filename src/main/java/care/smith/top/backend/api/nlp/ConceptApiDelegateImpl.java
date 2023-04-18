package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptApiDelegate;
import care.smith.top.backend.service.nlp.ConceptService;
import care.smith.top.model.Concept;
import care.smith.top.model.ConceptPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConceptApiDelegateImpl implements ConceptApiDelegate {

    @Autowired ConceptService conceptService;

    @Override
    public ResponseEntity<ConceptPage> getConceptsByDocumentId(
            String documentId, List<String> include, String name, Integer page) {
        return ConceptApiDelegate.super.getConceptsByDocumentId(documentId, include, name, page);
    }

    @Override
    public ResponseEntity<Concept> getConceptById(
            String conceptId, List<String> include) {
        return ConceptApiDelegate.super.getConceptById(conceptId, include);
    }

    @Override
    public ResponseEntity<ConceptPage> getConceptsByPhraseId(
            String phraseId, List<String> include, String name, Integer page) {
        return ConceptApiDelegate.super.getConceptsByPhraseId(phraseId, include, name, page);
    }

    @Override
    public ResponseEntity<List<Concept>> getConcepts(
            String phraseText) {
        //ToDo: filter by phraseText
        return ResponseEntity.ok(conceptService.concepts());
    }
}
