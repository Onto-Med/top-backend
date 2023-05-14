package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.PhraseApiDelegate;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.model.Phrase;
import care.smith.top.model.PhrasePage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhraseApiDelegateImpl implements PhraseApiDelegate {

    @Autowired PhraseService phraseService;

    @Override
    public ResponseEntity<PhrasePage> getPhrasesByConceptClusterIds(String conceptId, List<String> include, String name, Integer page) {
        //ToDo: add filtering by phraseText --> name
        List<Phrase> result = phraseService.getPhrasesForConcept(conceptId);
        return ResponseEntity.ok(
                (PhrasePage)
                        new PhrasePage()
                                .content(result)
                                .number(1)
                                .size(result.size())
                                .totalElements((long) result.size())
                                .totalPages(1));
    }

    @Override
    public ResponseEntity<PhrasePage> getPhrasesByDocumentId(String documentId, List<String> include, String name, Integer page) {
        return PhraseApiDelegate.super.getPhrasesByDocumentId(documentId, include, name, page);
    }

  @Override
  public ResponseEntity<Phrase> getPhraseById(String phraseId, List<String> include) {
    return PhraseApiDelegate.super.getPhraseById(phraseId, include);
  }

  @Override
  public ResponseEntity<List<Phrase>> getPhrases(String text, String conceptText) {
    // ToDo: add filtering by conceptText
    return new ResponseEntity<>(phraseService.getPhraseByText(text), HttpStatus.OK);
  }
}
