package care.smith.top.backend.model.neo4j;

import care.smith.top.backend.util.DocumentOffset;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class OffsetEntity {
  @JsonIgnore private static final Logger logger = Logger.getLogger(OffsetEntity.class.getName());
  @JsonIgnore private static final ObjectMapper mapper = new ObjectMapper();
  private List<List<Integer>> offsets;

  public OffsetEntity() {
    this.offsets = new ArrayList<>();
  }

  public OffsetEntity(List<List<Integer>> offsets) {
    if (offsets != null) this.offsets = offsets;
    else this.offsets = new ArrayList<>();
  }

  public List<DocumentOffset> getOffsets() {
    return this.offsets.stream().map(l -> new DocumentOffset(l.get(0), l.get(1))).toList();
  }

  public OffsetEntity addOffset(Integer begin, Integer end) {
    if (this.offsets == null) this.offsets = new ArrayList<>();
    if (begin != null && end != null) this.offsets.add(Arrays.asList(begin, end));
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
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      logger.warning(
          "Couldn't write offset in proper JSON string. Try to imitate with String concatenation.");
    }
    return "{"
        + this.offsets.stream()
            .map(l -> String.format("[%s,%s]", l.get(0), l.get(1)))
            .collect(Collectors.joining(","))
        + "}";
  }

  public static OffsetEntity fromJsonString(String jsonString) {
    try {
      return mapper.readValue(jsonString, OffsetEntity.class);
    } catch (JsonProcessingException e) {
      logger.warning(
          String.format(
              "Couldn't read offsets from JSON string\n'%s'\nTrying to manually parse it.",
              jsonString));
    }
    // ToDo: parse
    return new OffsetEntity();
  }
}
