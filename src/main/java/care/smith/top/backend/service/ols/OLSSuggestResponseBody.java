package care.smith.top.backend.service.ols;

import care.smith.top.backend.service.OLSCodeService;

import java.util.Arrays;
import java.util.Map;

/**
 * @author ralph
 */
public class OLSSuggestResponseBody {
  int numFound;
  private int start;
  private OLSSuggestResponseItem[] docs;

  public OLSSuggestResponseItem[] getDocs() {
    return docs;
  }

  public void setDocs(OLSSuggestResponseItem[] docs) {
    this.docs = docs;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getNumFound() {
    return numFound;
  }

  public void setNumFound(int numFound) {
    this.numFound = numFound;
  }

  void merge(Map<String, OLSAutoSuggestion> highlighting) {
    Arrays.stream(docs).forEach(doc -> doc.setAutoSuggestion(highlighting.get(doc.getId())));
  }
}
