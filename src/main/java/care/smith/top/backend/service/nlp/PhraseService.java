package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.neo4j.PhraseNodeEntity;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Phrase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Property;
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
    return Cypher.match(phrase.relationshipFrom(document, "HAS_PHRASE"))
        .returningDistinct(phrase)
        .build();
  }

  static Statement phraseInConcept(String conceptId) {
    Node phrase = Cypher.node("Phrase").named("phrase");
    Node concept =
        Cypher.node("Concept")
            .withProperties("conceptId", Cypher.literalOf(conceptId))
            .named("concept");
    return Cypher.match(phrase.relationshipTo(concept, "IN_CONCEPT"))
        .returningDistinct(phrase)
        .build();
  }

  static Statement phraseWithText(String text) {
    Node phrase =
        Cypher.node("Phrase").named("phrase");
    return Cypher.match(phrase)
        .where(phrase.property("phrase").matches(String.format("(?i).*%s.*", text)))
        .returningDistinct(phrase)
        .build();
  }

  static Statement phraseWithTextExactMatch(String text) {
      Node phrase =
          Cypher.node("Phrase")
              .withProperties("phrase", Cypher.literalOf(text))
              .named("phrase");
      return Cypher.match(phrase)
          .returning(phrase)
          .build();
    }

  static Statement phraseWithExactId(String phraseId) {
    Node phrase =
        Cypher.node("Phrase")
            .withProperties("phraseId", Cypher.literalOf(phraseId))
            .named("phrase");
    return Cypher.match(phrase)
        .returning(phrase)
        .build();
  }

  private final Function<PhraseNodeEntity, Phrase> phraseMapper = phraseNodeEntity ->
      new Phrase()
          .id(phraseNodeEntity.phraseId())
          .text(phraseNodeEntity.phraseText())
          .exemplar(phraseNodeEntity.exemplar())
          .attributes(phraseNodeEntity.phraseAttributes());

  @Override
  @Cacheable("phraseCount")
  public long count() {
    return phraseRepository.count();
  }

  @Cacheable("phraseByConcept")
  public List<Phrase> getPhrasesForConcept(String conceptId) {
    return phraseRepository.findAll(phraseInConcept(conceptId)).stream()
        .map(phraseMapper)
        .collect(Collectors.toList());
  }

  public List<Phrase> getAllPhrases() {
    return phraseRepository.findAll().stream()
        .map(phraseMapper)
        .collect(Collectors.toList());
  }

  public Optional<Phrase> getPhraseById(String phraseId) {
    return phraseRepository.findOne(phraseWithExactId(phraseId)).map(phraseMapper);
  }

  public List<Phrase> getPhraseByText(String text, boolean exactMatch) {
    Collection<PhraseNodeEntity> phrases = new ArrayList<>();
    if (exactMatch) {
      Optional<PhraseNodeEntity> optionalPhrase = phraseRepository.findOne(phraseWithTextExactMatch(text));
      optionalPhrase.ifPresent(phrases::add);
    } else {
      phrases = phraseRepository.findAll(phraseWithText(text));
    }
    return phrases.stream().map(phraseMapper).collect(Collectors.toList());
  }

  public List<Phrase> getPhraseByExactText(String text) {
    return phraseRepository.findAll(phraseWithText(text)).stream()
        .map(phraseMapper)
        .collect(Collectors.toList());
  }

  public List<Phrase> getPhrasesForDocument(String documentId, Boolean mostImportantOnly) {
    //        return phraseRepository.findAll(phraseInDocument(documentId, mostImportantOnly))
    return phraseRepository.getPhrasesForDocument(documentId, mostImportantOnly).stream()
        .map(phraseMapper)
        .collect(Collectors.toList());
  }
}
