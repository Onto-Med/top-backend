package care.smith.top.backend.util.nlp;

import care.smith.top.model.Document;
import java.util.*;
import java.util.stream.Collectors;

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
    this(document, documentOffsets, startTag, endTag, 0);
  }

  public DocumentRepresentation(
      Document document,
      List<DocumentOffset> documentOffsets,
      String startTag,
      String endTag,
      Integer tagPriority) {
    this.document = document;
    Integer hash = (startTag + endTag).hashCode();
    documentOffsets.forEach(
        offset ->
            offsetHighlightMap.put(
                hash, new HashMap<>(Map.of(offset, new Tag(startTag, endTag, tagPriority)))));
  }

  public Document getDocument() {
    return this.document;
  }

  public DocumentRepresentation replaceHighlightForOffset(
      DocumentOffset offset, String startTag, String endTag, Integer tagPriority) {
    Integer hash = (startTag + endTag).hashCode();
    for (Integer key : List.copyOf(offsetHighlightMap.keySet())) {
      if (offsetHighlightMap.get(key).containsKey(offset)) {
        offsetHighlightMap.get(key).remove(offset);
        if (offsetHighlightMap.get(key).isEmpty()) offsetHighlightMap.remove(key);
        offsetHighlightMap.put(
            hash, new HashMap<>(Map.of(offset, new Tag(startTag, endTag, tagPriority))));
      }
    }
    return this;
  }

  public DocumentRepresentation replaceHighlightForOffset(
      DocumentOffset offset, String startTag, String endTag) {
    return replaceHighlightForOffset(offset, startTag, endTag, 0);
  }

  public DocumentRepresentation addHighlightForOffset(
      DocumentOffset offset, String startTag, String endTag, Integer tagPriority) {
    for (Integer key : List.copyOf(offsetHighlightMap.keySet())) {
      if (offsetHighlightMap.containsKey(key) && offsetHighlightMap.get(key).containsKey(offset)) {
        return this;
      }
    }
    Integer hash = (startTag + endTag).hashCode();
    if (offsetHighlightMap.containsKey(hash)) {
      offsetHighlightMap.get(hash).put(offset, new Tag(startTag, endTag, tagPriority));
    } else {
      offsetHighlightMap.put(
          hash, new HashMap<>(Map.of(offset, new Tag(startTag, endTag, tagPriority))));
    }
    return this;
  }

  public DocumentRepresentation addHighlightForOffset(
      DocumentOffset offset, String startTag, String endTag) {
    return addHighlightForOffset(offset, startTag, endTag, 0);
  }

  public DocumentRepresentation addHighlightForOffsetsFromString(
      Iterable<String> offsets, Integer tagPriority) {
    return addHighlightForOffsetsFromString(offsets, "-", tagPriority);
  }

  public DocumentRepresentation addHighlightForOffsetsFromString(Iterable<String> offsets) {
    return addHighlightForOffsetsFromString(offsets, "-", 0);
  }

  public DocumentRepresentation addHighlightForOffsetsFromString(
      Iterable<String> offsets, String delimiter, Integer tagPriority) {
    //ToDo: here and where else applicable -> handle DocumentOffset.of if null
    offsets.forEach(
        off ->
            addHighlightForOffset(
                DocumentOffset.of(off, delimiter), defaultStartTag, defaultEndTag, tagPriority));
    return this;
  }

  public DocumentRepresentation addHighlightForOffsetsFromString(
      Iterable<String> offsets, String delimiter) {
    return addHighlightForOffsetsFromString(offsets, delimiter, 0);
  }

  public DocumentRepresentation addHighlightForOffsetsFromDocumentOffsets(
      Iterable<DocumentOffset> offsets) {
    offsets.forEach(offset -> addHighlightForOffset(offset, defaultStartTag, defaultEndTag));
    return this;
  }

  public DocumentRepresentation addHighlightForOffsetsFromDocumentOffsets(
      Iterable<DocumentOffset> offsets, String startTag, String endTag, Integer tagPriority) {
    offsets.forEach(offset -> addHighlightForOffset(offset, startTag, endTag, tagPriority));
    return this;
  }

  public DocumentRepresentation addHighlightForOffsetsFromDocumentOffsets(
      Iterable<DocumentOffset> offsets, String startTag, String endTag) {
    return addHighlightForOffsetsFromDocumentOffsets(offsets, startTag, endTag, 0);
  }

  public Document buildDocument() {
    String highlightedText =
        hasHighlightedText() ? buildTextWithHighlights() : getDocument().getText();
    return new Document()
        .id(getDocument().getId())
        .name(getDocument().getName())
        .score(getDocument().getScore())
        .text(getDocument().getText())
        .highlightedText("<div ref='documentText'>" + highlightedText + "</div>");
  }

  private String buildTextWithHighlights() {
    if (!hasHighlightedText()) return null;

    ArrayList<Map.Entry<DocumentOffset, Tag>> allOffsets =
        offsetHighlightMap.values().stream()
            .flatMap(m -> m.entrySet().stream())
            .map(e -> correctOffsets(e, getDocument().getText()))
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .collect(Collectors.toCollection(ArrayList::new));
    filterAndAdjustOffsets(allOffsets);
    StringBuilder textBuilder = new StringBuilder(getDocument().getText());
    allOffsets.forEach(
        entry -> {
          textBuilder.insert(entry.getKey().getEnd(), entry.getValue().getEndTag());
          textBuilder.insert(entry.getKey().getBegin(), entry.getValue().getStartTag());
        });

    return textBuilder.toString();
  }

  private void filterAndAdjustOffsets(ArrayList<Map.Entry<DocumentOffset, Tag>> offsets) {
    List<Map.Entry<DocumentOffset, Tag>> copy = List.copyOf(offsets);
    Set<DocumentOffset> removedOffsets = new HashSet<>();
    int t = 0;
    for (Map.Entry<DocumentOffset, Tag> thisEntry : copy) {
      ArrayList<DocumentOffset> toAdjust = new ArrayList<>();
      if (removedOffsets.contains(thisEntry.getKey())) continue;
      int n = t + 1;
      for (Map.Entry<DocumentOffset, Tag> nextEntry : copy.subList(n, offsets.size())) {
        if (removedOffsets.contains(nextEntry.getKey())) continue;
        // If this and next don't overlap the following next entries shouldn't be worth checking
        // either
        if (!thisEntry.getKey().overlaps(nextEntry.getKey())) break;
        // If this and the next element overlap but next is not just contained,
        // one of them will be removed depending on their tag priority
        if (thisEntry.getKey().overlaps(nextEntry.getKey())
            && !thisEntry.getKey().contains(nextEntry.getKey())) {
          if (thisEntry.getValue().getPriority() >= nextEntry.getValue().getPriority()) {
            removedOffsets.add(nextEntry.getKey());
            offsets.remove(nextEntry);
          } else {
            removedOffsets.add(thisEntry.getKey());
            offsets.remove(thisEntry);
            break;
          }
        }
        // If the next entry is only contained and doesn't overlap only the offsets need to be
        // adjusted since inserting this entry into the string beforehand will change the offsets
        else if (thisEntry.getKey().contains(nextEntry.getKey())) {
          toAdjust.add(offsets.get(n).getKey());
        }
        n++;
      }
      // Adjust the offsets of all following next if this was not removed
      if (!removedOffsets.contains(thisEntry.getKey()))
        toAdjust.forEach(
            offset -> offset.updateAdjustmentWith(thisEntry.getValue().getStartTag().length()));
      t++;
      if (t >= offsets.size() - 1) break;
    }
  }

  private Map.Entry<DocumentOffset, Tag> correctOffsets(
      Map.Entry<DocumentOffset, Tag> offset, String documentText) {
    Integer indexBefore = offset.getKey().getBegin() > 0 ? offset.getKey().getBegin() - 1 : null;
    Integer indexAfter =
        offset.getKey().getEnd() < documentText.length() - 1 ? offset.getKey().getEnd() + 1 : null;

    String charBefore =
        indexBefore != null ? documentText.substring(indexBefore, indexBefore + 1) : null;
    String charAtEnd =
        indexAfter != null ? documentText.substring(indexAfter, indexAfter + 1) : null;

    int moveOffset = 0;
    if (charBefore != null && charAtEnd != null && !(charBefore.isBlank() && charAtEnd.isBlank())) {
      moveOffset = moveOffset(documentText, indexBefore, indexAfter);
    }
    offset.getKey().updateBeginWith(moveOffset);
    offset.getKey().updateEndWith(moveOffset);
    return offset;
  }

  private int moveOffset(String documentText, Integer indexBefore, Integer indexAfter) {
    int left = checkForWhitespace(documentText, indexBefore + 1, -1);
    int right = checkForWhitespace(documentText, indexAfter - 1, 1);
    int move = 0;
    if (left < right) move = left != 0 ? -left : right;
    if (right < left) move = right != 0 ? right : -left;

    // ToDo: heuristic for move when both space to left and right is equal
    boolean leftIsWhitespace = checkForWhitespace(documentText, indexBefore + 1 + move, -1) == 0;
    boolean rightIsWhitespace = checkForWhitespace(documentText, indexAfter - 1 + move, 1) == 0;
    // ToDo: some odd cases where left and right are WS but moved to the wrong direction
    if (leftIsWhitespace && rightIsWhitespace) return move;
    return (Math.abs(move) == left ? right : -left);
  }

  private int checkForWhitespace(String documentText, Integer start, Integer direction) {
    int count = 0;
    int dir = direction < 0 ? -1 : 1;
    for (int i = start; true; i += dir) {
      if (i + dir <= 0 || i + dir > documentText.length()) return count;
      if (documentText.substring(Math.min(i, i + dir), Math.max(i, i + dir)).isBlank())
        return count;
      count++;
    }
  }

  private boolean hasHighlightedText() {
    return !this.offsetHighlightMap.isEmpty();
  }

  /**
   * priority: the lower, the better. Meaning if tags overlap the one with the lower number here is
   * preferred. However, when "getting" the priority a negative modifier is prepended to be more
   * logical to read: this.getPriority() < that.getPriority()
   */
  private static class Tag {
    private String startTag;
    private String endTag;
    private Integer priority;

    public Tag(String startTag, String endTag) {
      this.startTag = startTag;
      this.endTag = endTag;
      this.priority = 0;
    }

    public Tag(String startTag, String endTag, Integer priority) {
      this.startTag = startTag;
      this.endTag = endTag;
      this.priority = priority;
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

    public Integer getPriority() {
      return -priority;
    }

    public void setPriority(Integer priority) {
      this.priority = priority;
    }
  }
}
