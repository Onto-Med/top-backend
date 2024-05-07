package care.smith.top.backend.model.neo4j;

import care.smith.top.model.ConceptCluster;
import java.util.List;
import java.util.Set;
import org.springframework.data.neo4j.core.schema.*;

@Node("Concept")
public class ConceptNodeEntity {

  @Id @GeneratedValue Long id;

  @Property("conceptId")
  private final String conceptId;

  @Property("labels")
  private final List<String> labels;

  @Property("corpusId")
  private final String corpusId;

  @Relationship(type = "IN_CONCEPT", direction = Relationship.Direction.INCOMING)
  private Set<PhraseNodeEntity> conceptPhrases;

  public ConceptNodeEntity(
      String conceptId,
      String corpusId,
      List<String> labels,
      Set<PhraseNodeEntity> conceptPhrases) {
    this.conceptId = conceptId;
    this.corpusId = corpusId;
    this.labels = labels;
    this.conceptPhrases = conceptPhrases;
  }

  public String conceptId() {
    return this.conceptId;
  }

  public List<String> lables() {
    return this.labels;
  }

  public String corpusId() {
    return this.corpusId;
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
    return new ConceptCluster().id(this.conceptId).labels(this.labels);
  }

  public static ConceptNodeEntity nullConceptNode() {
    return new ConceptNodeEntity(null, null, null, null);
  }
}
