package care.smith.top.backend.service.ols;

import java.util.Arrays;
import java.util.Map;

/**
 * @author ralph
 */
public class OLSSuggestResponse {

  private OLSSuggestResponseBody response;

  private Map<String, OLSAutoSuggestion> highlighting;

  private boolean merged = false;

  public OLSSuggestResponseBody getResponse() {
    if (!merged) {
      response.merge(highlighting);
      merged = true;
    }
    return response;
  }

  public void setResponse(OLSSuggestResponseBody response) {
    this.response = response;
  }

  public Map<String, OLSAutoSuggestion> getHighlighting() {
    return highlighting;
  }

  public void setHighlighting(Map<String, OLSAutoSuggestion> highlighting) {
    this.highlighting = highlighting;
  }
}
