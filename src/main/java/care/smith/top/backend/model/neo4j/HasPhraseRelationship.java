package care.smith.top.backend.model.neo4j;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RelationshipProperties
public class HasPhraseRelationship {
  @GeneratedValue @Id
  private Long id;
  private String offsets;
  @TargetNode
  private PhraseNodeEntity phrase;

  public HasPhraseRelationship(PhraseNodeEntity phrase) {
    this.phrase = phrase;
    this.offsets = null;
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Integer begin, Integer end) {
    this.phrase = phrase;
    this.offsets = String.format("[[%s-%s]]", begin, end);
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Pair<Integer, Integer> offset) {
    this.phrase = phrase;
    this.offsets = String.format("[[%s-%s]]", offset.getLeft(), offset.getRight());
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Integer[] offset) {
    this.phrase = phrase;
    this.offsets = String.format("[[%s-%s]]", offset[0], offset[1]);
  }

  public HasPhraseRelationship(PhraseNodeEntity phrase, Iterable<int[]> offsets) {
    this.phrase = phrase;
    this.offsets = "[" + StreamSupport.stream(offsets.spliterator(), false).map(i -> String.format("[%s-%s]", i[0], i[1])).collect(Collectors.joining(",")) + "]";
  }

  public HasPhraseRelationship addOffset(int[] offset) {
    this.offsets = this.offsets.substring(0, this.offsets.length() - 1) + String.format(",[%s-%s]]", offset[0], offset[1]);
    return this;
  }

  public PhraseNodeEntity getPhrase() {
    return phrase;
  }

  public String getOffsetsAsString() {
    return offsets;
  }

  public List<int[]> getOffsetsAsInts() {
    return Arrays.stream(offsets.substring(1, this.offsets.length() - 1).split(",")).map(s -> {
      String[] sArray = s.substring(1, s.length() -1 ).split("-");
      return new int[]{Integer.parseInt(sArray[0]), Integer.parseInt(sArray[1])};
    }
    ).collect(Collectors.toList());
  }
}
