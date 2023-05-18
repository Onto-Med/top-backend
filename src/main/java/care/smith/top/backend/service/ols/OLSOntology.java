package care.smith.top.backend.service.ols;

import care.smith.top.backend.service.OLSCodeService;

/**
 * @author ralph
 */
public class OLSOntology {
    private String ontologyId;
    private OLSOntologyConfig config;

    public String getOntologyId() {
        return ontologyId;
    }

    public void setOntologyId(String ontologyId) {
        this.ontologyId = ontologyId;
    }

    public OLSOntologyConfig getConfig() {
        return config;
    }

    public void setConfig(OLSOntologyConfig config) {
        this.config = config;
    }
}
