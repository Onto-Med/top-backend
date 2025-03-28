package care.smith.top.backend.util.nlp;

import care.smith.top.model.Document;
import java.util.*;

public class DocumentRepresentation {
  private static final String defaultStartTag =
      "<span style=\"border: 2px solid black; padding: 3px; border-radius: 5px\">";
  private static final String defaultEndTag = "</span>";

  private final Document document;
  private final Map<Integer, Map<DocumentOffset, Tag>> offsetHighlightMap = new HashMap<>();

  public static DocumentRepresentation of(Document document) {
    return new DocumentRepresentation(document);
  }

  public DocumentRepresentation(Document document) {
    this(document, new ArrayList<>());
  }

  public DocumentRepresentation(Document document, List<DocumentOffset> documentOffsets) {
    this(document, documentOffsets, defaultStartTag, defaultEndTag);
  }

  public DocumentRepresentation(
      Document document, List<DocumentOffset> documentOffsets, String startTag, String endTag) {
    this.document = document;
    Integer hash = (startTag + endTag).hashCode();
    documentOffsets.forEach(
        offset ->
            offsetHighlightMap.put(hash, new HashMap<>(Map.of(offset, new Tag(startTag, endTag)))));
  }

  public Document getDocument() {
    return this.document;
  }

  public DocumentRepresentation replaceHighlightForOffset(
      DocumentOffset offset, String startTag, String endTag) {
    Integer hash = (startTag + endTag).hashCode();
    for (Integer key : List.copyOf(offsetHighlightMap.keySet())) {
      if (offsetHighlightMap.get(key).containsKey(offset)) {
        Tag tag = offsetHighlightMap.get(key).remove(offset);
        offsetHighlightMap.put(hash, new HashMap<>(Map.of(offset, tag)));
      }
    }
    return this;
  }

  public DocumentRepresentation addHighlightForOffset(
      DocumentOffset offset, String startTag, String endTag) {
    for (Integer key : List.copyOf(offsetHighlightMap.keySet())) {
      if (offsetHighlightMap.containsKey(key) && offsetHighlightMap.get(key).containsKey(offset)) {
        return this;
      }
    }
    Integer hash = (startTag + endTag).hashCode();
    if (offsetHighlightMap.containsKey(hash)) {
      offsetHighlightMap.get(hash).put(offset, new Tag(startTag, endTag));
    } else {
      offsetHighlightMap.put(hash, new HashMap<>(Map.of(offset, new Tag(startTag, endTag))));
    }
    return this;
  }

  public DocumentRepresentation addHighlightForOffsetsFromString(Iterable<String> offsets) {
    return addHighlightForOffsetsFromString(offsets, "-");
  }

  public DocumentRepresentation addHighlightForOffsetsFromString(
      Iterable<String> offsets, String delimiter) {
    offsets.forEach(
        off ->
            addHighlightForOffset(
                DocumentOffset.of(off, delimiter), defaultStartTag, defaultEndTag));
    return this;
  }

  public DocumentRepresentation addHighlightForOffsetsFromDocumentOffsets(
      Iterable<DocumentOffset> offsets) {
    offsets.forEach(offset -> addHighlightForOffset(offset, defaultStartTag, defaultEndTag));
    return this;
  }

  public DocumentRepresentation addHighlightForOffsetsFromDocumentOffsets(
      Iterable<DocumentOffset> offsets, String startTag, String endTag) {
    offsets.forEach(offset -> addHighlightForOffset(offset, startTag, endTag));
    return this;
  }

  public Document buildDocument() {
    return new Document()
        .id(getDocument().getId())
        .name(getDocument().getName())
        .score(getDocument().getScore())
        .text(getDocument().getText())
        .highlightedText(
            hasHighlightedText() ? buildTextWithHighlights() : getDocument().getText());
  }

  private String buildTextWithHighlights() {
    // ToDo: surrounding es highlights span with concept cluster highlight span needs to be seen
    //  if it works for all cases -> need to handle overlapping offsets!
    if (!hasHighlightedText()) return null;
    StringBuilder textBuilder = new StringBuilder(getDocument().getText());
    List<Map.Entry<DocumentOffset,Tag>> allOffsets = offsetHighlightMap.values().stream()
        .flatMap(m -> m.entrySet().stream())
        .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())).toList();

    for (int i = 0; i < allOffsets.size(); i++) {
      Map.Entry<DocumentOffset,Tag> thisEntry = allOffsets.get(i);
      Map.Entry<DocumentOffset,Tag> nextEntry = i < allOffsets.size() - 1 ? allOffsets.get(i + 1) : null;
      insertInStringBuilder(textBuilder, thisEntry, nextEntry);
    }
    return textBuilder.toString();
  }

  private void insertInStringBuilder(StringBuilder builder, Map.Entry<DocumentOffset,Tag> thisEntry, Map.Entry<DocumentOffset,Tag> nextEntry) {
    Integer indexBefore = thisEntry.getKey().getBegin() > 0 ? thisEntry.getKey().getBegin() - 1 : null;
    Integer indexAfter = thisEntry.getKey().getEnd() < builder.length() - 1 ? thisEntry.getKey().getEnd() + 1 : null;

    String charBefore = indexBefore != null ? builder.substring(indexBefore, indexBefore + 1): null;
    String charAtEnd = indexAfter != null ? builder.substring(indexAfter, indexAfter + 1): null;

    int moveOffset = 0;
    if (charBefore != null && charAtEnd != null && !(charBefore.isBlank() && charAtEnd.isBlank())) {
      moveOffset = moveOffset(builder, indexBefore, indexAfter);
    }
    builder.insert(thisEntry.getKey().getEnd() + moveOffset, thisEntry.getValue().getEndTag());
    builder.insert(thisEntry.getKey().getBegin() + moveOffset, thisEntry.getValue().getStartTag());
  }

  private int moveOffset(StringBuilder builder, Integer indexBefore, Integer indexAfter) {
    int left = checkForWhitespace(builder, indexBefore + 1, -1);
    int right = checkForWhitespace(builder, indexAfter - 1, 1);
    int move = 0;
    if (left < right) move = left != 0 ? -left : right;
    if (right < left) move = right != 0 ? right : -left;

    //ToDo: heuristic for move when both space to left and right is equal
    boolean leftIsWhitespace = checkForWhitespace(builder, indexBefore + 1 + move, -1) == 0;
    boolean rightIsWhitespace = checkForWhitespace(builder, indexAfter - 1 + move, 1) == 0;
    if (leftIsWhitespace && rightIsWhitespace) return move;
    return (Math.abs(move) == left ? right : -left);
  }

  private int checkForWhitespace(StringBuilder builder, Integer start, Integer direction) {
    int count = 0;
    int dir = direction < 0 ? -1 : 1;
    for (int i = start; true; i+=dir) {
      if (i + dir <= 0 || i + dir > builder.length()) return count;
      if (builder.substring(Math.min(i, i + dir), Math.max(i, i + dir)).isBlank()) return count;
      count++;
    }
  }

  private boolean hasHighlightedText() {
    return !this.offsetHighlightMap.isEmpty();
  }

  private static class Tag {
    private String startTag;
    private String endTag;

    public Tag(String startTag, String endTag) {
      this.startTag = startTag;
      this.endTag = endTag;
    }

    public String getStartTag() {
      return startTag;
    }

    public void setStartTag(String startTag) {
      this.startTag = startTag;
    }

    public String getEndTag() {
      return endTag;
    }

    public void setEndTag(String endTag) {
      this.endTag = endTag;
    }
  }
}
