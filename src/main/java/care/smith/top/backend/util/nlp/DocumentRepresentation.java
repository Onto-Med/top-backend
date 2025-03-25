package care.smith.top.backend.util.nlp;

import java.util.ArrayList;
import java.util.List;

public class DocumentRepresentation {
  private String originalText;
  private String markedText;
  private List<DocumentOffset> markedSpans;

  private class Tag {
    private String startTag;
    private String endTag;
  }
}
