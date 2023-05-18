package care.smith.top.backend.service.ols;

import java.util.List;

/**
 * @author ralph
 */
public class OLSAutoSuggestion {

    private List<String> label_autosuggest;

    private List<String> label;


    private List<String> synonym_autosuggest;

    private List<String> synonym;

    public List<String> getLabel_autosuggest() {
        return label_autosuggest;
    }

    public void setLabel_autosuggest(List<String> label_autosuggest) {
        this.label_autosuggest = label_autosuggest;
    }

    public List<String> getLabel() {
        return label;
    }

    public void setLabel(List<String> label) {
        this.label = label;
    }

    public List<String> getSynonym_autosuggest() {
        return synonym_autosuggest;
    }

    public void setSynonym_autosuggest(List<String> synonym_autosuggest) {
        this.synonym_autosuggest = synonym_autosuggest;
    }

    public List<String> getSynonym() {
        return synonym;
    }

    public void setSynonym(List<String> synonym) {
        this.synonym = synonym;
    }
}
