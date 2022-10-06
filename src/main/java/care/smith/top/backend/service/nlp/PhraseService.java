package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.nlp.PhraseRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Phrase;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class PhraseService implements ContentService {

    @Autowired PhraseRepository phraseRepository;

    @Override
    @Cacheable("phraseCount")
    public long count() { return phraseRepository.count(); }

    @Cacheable("phraseByConcept")
    public List<Phrase> getPhrasesByConcept(String concept) {
        return phraseRepository.findAll(phraseWithConcept(concept))
                .stream()
                .map(phraseEntity -> new Phrase()
                        .id(phraseEntity.phraseId())
                        .text(phraseEntity.phraseText())
                        .attributes(phraseEntity.phraseAttributes()))
                .collect(Collectors.toList());
    }

    public List<Phrase> getPhraseByText(String text) {
        return phraseRepository.findOne(phraseWithText(text))
                .stream()
                .map(phraseEntity -> new Phrase()
                        .id(phraseEntity.phraseId())
                        .text(phraseEntity.phraseText())
                        .attributes(phraseEntity.phraseAttributes()))
                .collect(Collectors.toList());
    }

    static Statement phraseWithConcept(String concept) {
        Node phrase = Cypher.node("Phrase", "Concept_" + concept).named("phrase");
        return Cypher.match(phrase).returning(phrase).build();
    }

    static Statement phraseWithText(String text) {
        Node phrase = Cypher.node("Phrase").named("phrase")
                .withProperties("phrase", Cypher.literalOf(text));
        return Cypher.match(phrase).returning(phrase).build();
    }
}
