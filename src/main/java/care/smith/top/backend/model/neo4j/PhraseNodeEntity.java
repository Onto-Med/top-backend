package care.smith.top.backend.model.neo4j;

import care.smith.top.model.Phrase;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.neo4j.core.schema.*;

@Node("Phrase")
public class PhraseNodeEntity {

  @Property("attributes")
  private final List<String> phraseAttributes;

  @Property("exemplar")
  private final Boolean exemplar;

  @Property("phrase")
  private final String phraseText;

  @Id @GeneratedValue Long id;
  @Property("phraseId")
  private final String phraseId;

  @Relationship(type = "NEIGHBOR_OF")
  private Set<PhraseNodeEntity> phrases;

  public PhraseNodeEntity(
      List<String> phraseAttributes, Boolean exemplar, String phraseText, String phraseId) {
    this.phraseAttributes = phraseAttributes;
    this.exemplar = exemplar;
    this.phraseText = phraseText;
    this.phraseId = phraseId;
  }

  public List<String> phraseAttributes() {
    return this.phraseAttributes;
  }

  public String phraseText() {
    return this.phraseText;
  }

  public Boolean exemplar() {
    return this.exemplar;
  }

  public String phraseId() {
    return this.phraseId;
  }

  public PhraseNodeEntity addNeighbor(PhraseNodeEntity phraseNode) {
    if (this.phrases == null) this.phrases = new HashSet<>();
    this.phrases.add(phraseNode);
    return this;
  }

  public PhraseNodeEntity addNeighbors(Iterable<PhraseNodeEntity> phraseNodes) {
    phraseNodes.forEach(this::addNeighbor);
    return this;
  }

  public PhraseNodeEntity removeNeighbor(PhraseNodeEntity phraseNode) {
    this.phrases.remove(phraseNode);
    return this;
  }

  public PhraseNodeEntity removeAllNeighbors() {
    this.phrases.clear();
    return this;
  }

  public Phrase toApiModel() {
    return new Phrase()
        .id(this.phraseId)
        .text(this.phraseText)
        .exemplar(this.exemplar)
        .attributes(this.phraseAttributes);
  }
}
