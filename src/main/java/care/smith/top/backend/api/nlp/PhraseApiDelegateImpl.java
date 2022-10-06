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
    public ResponseEntity<List<Phrase>> getPhrases(List<String> include, String id, String text, String concept) {
        if (concept != null && Stream.of(include, id, text).allMatch(Objects::isNull)) {
            return new ResponseEntity<>(phraseService.getPhrasesByConcept(concept), HttpStatus.OK);
        } else if (text != null && Stream.of(include, id, concept).allMatch(Objects::isNull)) {
            return new ResponseEntity<>(phraseService.getPhraseByText(text), HttpStatus.OK);
        }
        return new ResponseEntity<>(List.of(), HttpStatus.OK);
    }
}
