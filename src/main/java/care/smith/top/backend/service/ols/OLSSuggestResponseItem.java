package care.smith.top.backend.service.ols;

import java.net.URI;

/**
 * @author ralph
 */
public class OLSSuggestResponseItem {
  private String id;
  private URI iri;

  private String label;

  private OLSAutoSuggestion autoSuggestion;

  public URI getIri() {
    return iri;
  }

  public void setIri(URI iri) {
    this.iri = iri;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OLSAutoSuggestion getAutoSuggestion() {
    return autoSuggestion;
  }

  public void setAutoSuggestion(OLSAutoSuggestion autoSuggestion) {
    this.autoSuggestion = autoSuggestion;
  }
}
