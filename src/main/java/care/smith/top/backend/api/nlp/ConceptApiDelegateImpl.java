package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptApiDelegate;
import care.smith.top.backend.service.nlp.ConceptService;
import care.smith.top.model.Concept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConceptApiDelegateImpl implements ConceptApiDelegate {

    @Autowired ConceptService conceptService;

    @Override
    public ResponseEntity<List<Concept>> getConcepts(List<String> include, String id, String phrase) {
        return new ResponseEntity<>(conceptService.concepts(), HttpStatus.OK);
    }
}
