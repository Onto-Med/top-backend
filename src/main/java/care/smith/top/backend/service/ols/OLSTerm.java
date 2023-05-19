package care.smith.top.backend.service.ols;

import java.util.List;

/**
 * @author ralph
 */
public class OLSTerm {
  private String iri;
  private String label;
  private String description;
  private String ontology_name;
  private String ontology_prefix;
  private String ontology_iri;
  private List<String> synonyms;

  public String getIri() {
    return iri;
  }

  public void setIri(String iri) {
    this.iri = iri;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOntology_name() {
    return ontology_name;
  }

  public void setOntology_name(String ontology_name) {
    this.ontology_name = ontology_name;
  }

  public String getOntology_prefix() {
    return ontology_prefix;
  }

  public void setOntology_prefix(String ontology_prefix) {
    this.ontology_prefix = ontology_prefix;
  }

  public String getOntology_iri() {
    return ontology_iri;
  }

  public void setOntology_iri(String ontology_iri) {
    this.ontology_iri = ontology_iri;
  }

  public List<String> getSynonyms() {
    return synonyms;
  }

  public void setSynonyms(List<String> synonyms) {
    this.synonyms = synonyms;
  }
}
