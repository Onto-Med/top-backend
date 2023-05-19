package care.smith.top.backend.service.ols;

import java.net.URI;

/**
 * @author ralph
 */
public class OLSOntologyConfig {
  private URI id;
  private String title;
  private String description;

  public URI getId() {
    return id;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
