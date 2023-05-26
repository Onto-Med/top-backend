package care.smith.top.backend.service.ols;

import java.net.URI;
import java.util.List;

/**
 * @author ralph
 */
public class OLSSuggestResponseItem {
  private String id;
  private URI iri;
  private String label;
  private String ontology_name;

  // although it's an array, OLS uses the singular form
  private List<String> synonym;

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

  public String getOntology_name() {
    return ontology_name;
  }

  public void setOntology_name(String ontology_name) {
    this.ontology_name = ontology_name;
  }

  public List<String> getSynonym() {
    return synonym;
  }

  public void setSynonym(List<String> synonym) {
    this.synonym = synonym;
  }

  public OLSAutoSuggestion getAutoSuggestion() {
    return autoSuggestion;
  }

  public void setAutoSuggestion(OLSAutoSuggestion autoSuggestion) {
    this.autoSuggestion = autoSuggestion;
  }
}
