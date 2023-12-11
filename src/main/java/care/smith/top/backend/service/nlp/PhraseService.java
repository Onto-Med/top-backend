package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Phrase;
import java.util.List;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PhraseService implements ContentService {

  private final PhraseNodeRepository phraseRepository;

  public PhraseService(PhraseNodeRepository phraseRepository) {
    this.phraseRepository = phraseRepository;
  }

  static Statement phraseInDocument(String documentId, Boolean mostImportantOnly) {
    Node phrase =
        Cypher.node("Phrase")
            .withProperties("exemplar", Cypher.literalOf(mostImportantOnly))
            .named("phrase");
    Node document =
        Cypher.node("Document")
            .withProperties("docId", Cypher.literalOf(documentId))
            .named("document");
    return Cypher.match(phrase.relationshipFrom(document, "HAS_PHRASE")).returning(phrase).build();
  }

  static Statement phraseInConcept(String conceptId) {
    Node phrase = Cypher.node("Phrase").named("phrase");
    Node concept =
        Cypher.node("Concept")
            .withProperties("conceptId", Cypher.literalOf(conceptId))
            .named("concept");
    return Cypher.match(phrase.relationshipTo(concept, "IN_CONCEPT")).returning(phrase).build();
  }

  static Statement phraseWithText(String text) {
    Node phrase =
        Cypher.node("Phrase").named("phrase").withProperties("phrase", Cypher.literalOf(text));
    return Cypher.match(phrase).returning(phrase).build();
  }

  @Override
  @Cacheable("phraseCount")
  public long count() {
    return phraseRepository.count();
  }

  @Cacheable("phraseByConcept")
  public List<Phrase> getPhrasesForConcept(String conceptId) {
    return phraseRepository.findAll(phraseInConcept(conceptId)).stream()
        .map(
            phraseEntity ->
                new Phrase()
                    .id(phraseEntity.phraseId())
                    .text(phraseEntity.phraseText())
                    .attributes(phraseEntity.phraseAttributes()))
        .collect(Collectors.toList());
  }

  public List<Phrase> getPhraseByText(String text) {
    return phraseRepository.findOne(phraseWithText(text)).stream()
        .map(
            phraseEntity ->
                new Phrase()
                    .id(phraseEntity.phraseId())
                    .text(phraseEntity.phraseText())
                    .attributes(phraseEntity.phraseAttributes()))
        .collect(Collectors.toList());
  }

  public List<Phrase> getPhrasesForDocument(String documentId, Boolean mostImportantOnly) {
    //        return phraseRepository.findAll(phraseInDocument(documentId, mostImportantOnly))
    return phraseRepository.getPhrasesForDocument(documentId, mostImportantOnly).stream()
        .map(
            phraseEntity ->
                new Phrase()
                    .id(phraseEntity.phraseId())
                    .text(phraseEntity.phraseText())
                    .attributes(phraseEntity.phraseAttributes()))
        .collect(Collectors.toList());
  }
}
