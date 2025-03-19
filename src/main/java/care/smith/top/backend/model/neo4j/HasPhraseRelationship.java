package care.smith.top.backend.model.neo4j;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class HasPhraseRelationship {
  @RelationshipId
  private Long id;
  private Integer begin;
  private Integer end;
  @TargetNode
  private PhraseNodeEntity phrase;

  public HasPhraseRelationship(PhraseNodeEntity phrase) {
    this.phrase = phrase;
    this.begin = null;
    this.end = null;
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Integer begin, Integer end) {
    this.phrase = phrase;
    this.begin = begin;
    this.end = end;
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Pair<Integer, Integer> offset) {
    this.phrase = phrase;
    this.begin = offset.getLeft();
    this.end = offset.getRight();
  }

  public PhraseNodeEntity getPhrase() {
    return phrase;
  }

  public Integer getBegin() {
    return begin;
  }

  public Integer getEnd() {
    return end;
  }

  public Pair<Integer, Integer> getOffset() {
    return new ImmutablePair<>(begin, end);
  }
}
