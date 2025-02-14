package care.smith.top.backend.service.ols;

import java.util.Collections;
import java.util.List;

/**
 * @author ralph
 */
public class OLSTerm {
  private String iri;
  private String short_form;
  private String label;
  private List<String> description = Collections.emptyList();
  private String ontology_name;
  private String ontology_prefix;
  private String ontology_iri;
  private List<String> synonyms = Collections.emptyList();
  private OLSLinks _links;

  public String getIri() {
    return iri;
  }

  public void setIri(String iri) {
    this.iri = iri;
  }

  public String getShort_form() {
    return short_form;
  }

  public void setShort_form(String short_form) {
    this.short_form = short_form;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<String> getDescription() {
    return description;
  }

  public void setDescription(List<String> description) {
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

  public OLSLinks get_links() {
    return _links;
  }

  public void set_links(OLSLinks _links) {
    this._links = _links;
  }
}
