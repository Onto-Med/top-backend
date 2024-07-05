package care.smith.top.backend.service.ols;

/**
 * @author Ralph Schäfermeier
 */
public class OLSHierarchicalChildrenResponse {
  private OLSHierarchicalChildrenEmbedded _embedded;
  private OLSPage page;
  
  public OLSHierarchicalChildrenEmbedded get_embedded() {
    return _embedded;
  }
  
  public void set_embedded(OLSHierarchicalChildrenEmbedded _embedded) {
    this._embedded = _embedded;
  }
  
  public OLSPage getPage() {
    return page;
  }
  
  public void setPage(OLSPage page) {
    this.page = page;
  }
}
