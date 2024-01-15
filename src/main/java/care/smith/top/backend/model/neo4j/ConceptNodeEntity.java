package care.smith.top.backend.model.neo4j;

import care.smith.top.model.ConceptCluster;
import java.util.List;
import java.util.Set;
import org.springframework.data.neo4j.core.schema.*;

@Node("Concept")
public class ConceptNodeEntity {

  @Id
  @Property("conceptId")
  private final String conceptId;

  @Property("labels")
  private final List<String> labels;

  @Relationship(type = "IN_CONCEPT", direction = Relationship.Direction.INCOMING)
  private Set<PhraseNodeEntity> conceptPhrases;

  public ConceptNodeEntity(
      String conceptId, List<String> labels, Set<PhraseNodeEntity> conceptPhrases) {
    this.conceptId = conceptId;
    this.labels = labels;
    this.conceptPhrases = conceptPhrases;
  }

  public String conceptId() {
    return this.conceptId;
  }

  public List<String> lables() {
    return this.labels;
  }

  public Set<PhraseNodeEntity> conceptPhrases() {
    return this.conceptPhrases;
  }

  public ConceptNodeEntity addPhrase(PhraseNodeEntity phraseNode) {
    this.conceptPhrases.add(phraseNode);
    return this;
  }

  public ConceptNodeEntity removePhrase(PhraseNodeEntity phraseNode) {
    this.conceptPhrases.remove(phraseNode);
    return this;
  }

  public ConceptCluster toApiModel() {
    return new ConceptCluster().id(this.conceptId).labels(String.join(";", this.labels));
  }
}
