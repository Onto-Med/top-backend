package care.smith.top.backend.util.nlp;

import care.smith.top.model.Document;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DocumentRepresentation {
  private static final String defaultStartTag = "<span style=\"border: 2px solid black; padding: 3px; border-radius: 5px\">";
  private static final String defaultEndTag = "</span>";

  private String originalText;
  private String markedText;
  private List<DocumentOffset> documentOffsets;
  private Map<Integer, Map<DocumentOffset, Tag>> offsetHighlightMap;

  public DocumentRepresentation(Document document, List<DocumentOffset> documentOffsets) {
    this.originalText = document.getHighlightedText();
    this.documentOffsets = documentOffsets;
    documentOffsets.forEach(offset -> this.offsetHighlightMap.put(0, Map.of(offset, new Tag(defaultStartTag, defaultEndTag))));
  }

  public void replaceHighlightForOffset(DocumentOffset offset, String startTag, String endTag) {
    if (!offsetHighlightMap.containsKey(offset)) return;
    this.offsetHighlightMap.get(0).put(offset, new Tag(startTag, endTag));
  }

  public void addHighlightForOffset(DocumentOffset offset, String startTag, String endTag) {
    for (int i = 0; i < this.offsetHighlightMap.size(); i++) {
      if (this.offsetHighlightMap.containsKey(i) && !this.offsetHighlightMap.get(i).containsKey(offset)) {
        this.offsetHighlightMap.get(i).put(offset, new Tag(startTag, endTag));
        break;
      } else if (!this.offsetHighlightMap.containsKey(i)) {
        this.offsetHighlightMap.put(i, Map.of(offset, new Tag(startTag, endTag)));
        break;
      }
    }
  }

  private void buildTextWithHighlights() {
    // ToDo: surrounding es highlights span with concept cluster highlight span needs to be seen if
    //  it works for all cases -> need to handle overlapping offsets!
    if (this.offsetHighlightMap == null || this.offsetHighlightMap.isEmpty()) return;
    StringBuilder textBuilder = new StringBuilder(this.originalText);
    this.offsetHighlightMap.keySet().stream()
        .sorted((Comparator.reverseOrder()))
        .forEach(
            offset -> {
              textBuilder.insert(offset.getEnd(), offsetHighlightMap.get(offset).getEndTag());
              textBuilder.insert(offset.getBegin(), offsetHighlightMap.get(offset).getStartTag());
            });
    this.markedText = textBuilder.toString();
  }

  private class Tag {
    private String startTag;
    private String endTag;

    public Tag(String startTag, String endTag) {}

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
