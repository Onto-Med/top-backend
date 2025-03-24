package care.smith.top.backend.model.neo4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class HasPhraseRelationship {
  private static final Logger logger = Logger.getLogger(HasPhraseRelationship.class.getName());

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

  public OffsetEntity toOffsetEntity() {
    try {
      return new ObjectMapper().readValue(this.getOffsets(), OffsetEntity.class);
    } catch (JsonProcessingException e) {
      logger.warning(
          String.format(
              "Couldn't read offsets from JSON string\n'%s'\nTrying to manually parse it.",
              this.getOffsets()));
    }
    // ToDo: parse
    return new OffsetEntity();
  }
}
