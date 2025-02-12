package care.smith.top.backend.service.ols;

import java.net.URI;

/**
 * @author ralph
 */
public class OLSOntologyConfig {
  private URI id;
  private URI versionId;
  private String title;
  private String description;
  private String preferredPrefix;

  public URI getId() {
    return id;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public URI getVersionId() {
    return versionId;
  }

  public void setVersionId(URI versionId) {
    this.versionId = versionId;
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

  public String getPreferredPrefix() {
    return preferredPrefix;
  }

  public void setPreferredPrefix(String preferredPrefix) {
    this.preferredPrefix = preferredPrefix;
  }
}
