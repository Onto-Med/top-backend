package care.smith.top.backend.model.neo4j;

import care.smith.top.backend.util.nlp.DocumentOffset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.NotImplementedException;

public class OffsetEntity {
  private static final Logger logger = Logger.getLogger(OffsetEntity.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();
  private List<DocumentOffset> offsets;

  public OffsetEntity() {
    this.offsets = new ArrayList<>();
  }

  public OffsetEntity(List<DocumentOffset> offsets) {
    this.offsets = offsets;
  }

  public OffsetEntity(Iterable<List<Integer>> offsets) {
    this.offsets = new ArrayList<>();
    if (offsets != null)
      offsets.forEach(
          list -> {
            if (list.size() >= 2) this.offsets.add(DocumentOffset.of(list));
          });
  }

  public List<DocumentOffset> getOffsets() {
    return this.offsets;
  }

  public void setOffsets(List<DocumentOffset> offsets) {
    this.offsets = offsets;
  }

  public OffsetEntity addOffset(Integer begin, Integer end) {
    if (this.offsets == null) this.offsets = new ArrayList<>();
    if (begin != null && end != null) this.offsets.add(new DocumentOffset(begin, end));
    return this;
  }

  public OffsetEntity addOffset(List<Integer> offset) {
    if (offset != null && offset.size() >= 2) this.addOffset(offset.get(0), offset.get(1));
    return this;
  }

  public OffsetEntity addOffsets(Iterable<List<Integer>> offsets) {
    if (offsets != null)
      StreamSupport.stream(offsets.spliterator(), false).forEach(this::addOffset);
    return this;
  }

  public String toJsonString() {
    try {
      return mapper.writeValueAsString(this.getOffsets());
    } catch (JsonProcessingException e) {
      logger.warning(
          "Couldn't write offset in proper JSON string. Try to imitate with String concatenation.");
    }
    return "["
        + this.getOffsets().stream()
            .map(l -> String.format("{\"begin\":%s,\"end\":%s}", l.getBegin(), l.getEnd()))
            .collect(Collectors.joining(","))
        + "]";
  }

  public static OffsetEntity fromJsonString(String jsonString) {
    try {
      return new OffsetEntity(
          mapper.readValue(jsonString, new TypeReference<List<DocumentOffset>>() {}));
    } catch (JsonProcessingException e) {
      logger.warning(
          String.format(
              "Couldn't read offsets from JSON string\n'%s'\nTrying to manually parse it.",
              jsonString));
    }
    throw new NotImplementedException();
    //    return new OffsetEntity();
  }
}
