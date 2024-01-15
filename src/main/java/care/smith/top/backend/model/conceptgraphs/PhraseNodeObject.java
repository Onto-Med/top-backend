package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.backend.model.neo4j.PhraseNodeEntity;

public class PhraseNodeObject {
  private String id;
  private String label;
  private String[] documents;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String[] getDocuments() {
    return documents;
  }

  public void setDocuments(String[] documents) {
    this.documents = documents;
  }

  public PhraseNodeEntity toPhraseNodeEntity() {
    return new PhraseNodeEntity(null, false, this.getLabel(), this.getId());
  }
}
