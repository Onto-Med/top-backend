package care.smith.top.backend.model.conceptgraphs;

public class AdjacencyObject {
  private String id;
  private PhraseNodeNeighbors[] neighbors;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PhraseNodeNeighbors[] getNeighbors() {
    return neighbors;
  }

  public void setNeighbors(PhraseNodeNeighbors[] neighbors) {
    this.neighbors = neighbors;
  }
}
