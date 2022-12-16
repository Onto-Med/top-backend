package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.PhraseApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.model.Phrase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class PhraseApiDelegateImpl implements PhraseApiDelegate {

    @Autowired PhraseService phraseService;

    @Override
    public ResponseEntity<List<Phrase>> getPhraseByConceptId(String conceptId, List<String> include, String name, Integer page) {
        //ToDo: add filtering by phraseText --> name
        return new ResponseEntity<>(phraseService.getPhrasesByConcept(conceptId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Phrase>> getPhraseByDocumentId(String documentId, List<String> include, String name, Integer page) {
        return PhraseApiDelegate.super.getPhraseByDocumentId(documentId, include, name, page);
    }

    @Override
    public ResponseEntity<Phrase> getPhraseById(String phraseId, List<String> include) {
        return PhraseApiDelegate.super.getPhraseById(phraseId, include);
    }

    @Override
    public ResponseEntity<List<Phrase>> getPhrases(String text, String conceptText) {
        //ToDo: add filtering by conceptText
        return new ResponseEntity<>(phraseService.getPhraseByText(text), HttpStatus.OK);
    }
}