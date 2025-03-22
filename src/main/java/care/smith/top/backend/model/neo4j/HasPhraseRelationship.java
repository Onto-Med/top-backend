package care.smith.top.backend.model.neo4j;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
