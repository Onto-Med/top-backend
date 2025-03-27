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
    offsetHighlightMap.values().stream()
        .flatMap(m -> m.entrySet().stream())
        .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
        .forEach(
            entry -> {
              textBuilder.insert(entry.getKey().getEnd(), entry.getValue().getEndTag());
              textBuilder.insert(entry.getKey().getBegin(), entry.getValue().getStartTag());
            });
    return textBuilder.toString();
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
