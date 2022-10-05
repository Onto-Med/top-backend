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

@Service
public class PhraseApiDelegateImpl implements PhraseApiDelegate {

    @Autowired PhraseService phraseService;

    @Override
    public ResponseEntity<List<Phrase>> getPhrases(List<String> include, String id, String text) {
        return new ResponseEntity<>(phraseService.getPhrases(), HttpStatus.OK);
    }
}
