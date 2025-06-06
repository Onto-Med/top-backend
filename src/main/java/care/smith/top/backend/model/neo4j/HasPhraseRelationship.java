package care.smith.top.backend.model.neo4j;

import java.util.List;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class HasPhraseRelationship {
  @GeneratedValue @Id private Long id;

  @Property("offsets")
  private String offsets;

  @TargetNode private PhraseNodeEntity phrase;

  @PersistenceCreator
  public HasPhraseRelationship(PhraseNodeEntity phrase, String offsets) {
    this.phrase = phrase;
    this.offsets = offsets;
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase) {
    this.phrase = phrase;
    this.offsets = null;
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Integer begin, Integer end) {
    this.phrase = phrase;
    this.offsets = new OffsetEntity().addOffset(begin, end).toJsonString();
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, List<Integer> offset) {
    this.phrase = phrase;
    this.offsets = new OffsetEntity().addOffset(offset).toJsonString();
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Iterable<List<Integer>> offsets) {
    this.phrase = phrase;
    this.offsets = new OffsetEntity().addOffsets(offsets).toJsonString();
  }

  public PhraseNodeEntity getPhrase() {
    return phrase;
  }

  public String getOffsets() {
    return offsets;
  }
}
