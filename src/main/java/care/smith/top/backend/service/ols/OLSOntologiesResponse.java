package care.smith.top.backend.service.ols;

import care.smith.top.backend.service.OLSCodeService;

/**
 * @author ralph
 */
public class OLSOntologiesResponse {
  private OLSOntologyEmbedded _embedded;
  private OLSPage page;

  public OLSOntologyEmbedded get_embedded() {
    return _embedded;
  }

  public void set_embedded(OLSOntologyEmbedded _embedded) {
    this._embedded = _embedded;
  }

  public OLSPage getPage() {
    return page;
  }

  public void setPage(OLSPage page) {
    this.page = page;
  }
}
