package care.smith.top.backend.model.neo4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import care.smith.top.model.Phrase;
import org.springframework.data.neo4j.core.schema.*;

@Node("Phrase")
public class PhraseNodeEntity {

  @Property("attributes")
  private final List<String> phraseAttributes;

  @Property("exemplar")
  private final Boolean isExemplar;

  @Property("phrase")
  private final String phraseText;

  @Property("phraseId")
  private final String phraseId;

  @Id @GeneratedValue private Long id;
  @Relationship(type = "NEIGHBOR_OF")
  private Set<PhraseNodeEntity> phrases;

  public PhraseNodeEntity(
      List<String> phraseAttributes, Boolean isExemplar, String phraseText, String phraseId) {
    this.id = null;
    this.phraseAttributes = phraseAttributes;
    this.isExemplar = isExemplar;
    this.phraseText = phraseText;
    this.phraseId = phraseId;
  }

  public PhraseNodeEntity withId(Long id) {
    if (this.id.equals(id)) {
      return this;
    } else {
      PhraseNodeEntity newObj =
          new PhraseNodeEntity(
              this.phraseAttributes, this.isExemplar, this.phraseText, this.phraseId);
      newObj.id = id;
      return newObj;
    }
  }

  public List<String> phraseAttributes() {
    return this.phraseAttributes;
  }

  public String phraseText() {
    return this.phraseText;
  }

  public Boolean isExemplar() {
    return this.isExemplar;
  }

  public String phraseId() {
    return this.phraseId;
  }

  public Long nodeId() {
    return this.id;
  }

  public PhraseNodeEntity addNeighbor(PhraseNodeEntity phraseNode){
    if (this.phrases == null) this.phrases = new HashSet<>();
    this.phrases.add(phraseNode);
    return this;
  }

  public PhraseNodeEntity addNeighbors(Iterable<PhraseNodeEntity> phraseNodes){
    phraseNodes.forEach(this::addNeighbor);
    return this;
  }

  public PhraseNodeEntity removeNeighbor(PhraseNodeEntity phraseNode){
    this.phrases.remove(phraseNode);
    return this;
  }

  public PhraseNodeEntity removeAllNeighbors(){
    this.phrases.clear();
    return this;
  }

  public Phrase toApiModel(){
    return new Phrase()
        .id(this.phraseId)
        .text(this.phraseText)
        .isExemplar(this.isExemplar)
        .attributes(this.phraseAttributes);
  }
}
