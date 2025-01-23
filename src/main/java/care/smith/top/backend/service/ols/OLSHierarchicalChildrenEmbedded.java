package care.smith.top.backend.service.ols;

import java.util.Collections;
import java.util.List;

/**
 * @author Ralph Schäfermeier
 */
public class OLSHierarchicalChildrenEmbedded {
  private List<OLSTerm> terms = Collections.emptyList();

  public List<OLSTerm> getTerms() {
    return terms;
  }

  public void setTerms(List<OLSTerm> terms) {
    this.terms = terms;
  }
}
