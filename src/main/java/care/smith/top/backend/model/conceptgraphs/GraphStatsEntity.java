package care.smith.top.backend.model.conceptgraphs;

public class GraphStatsEntity {
  private String id;
  private int edges;
  private int nodes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getEdges() {
    return edges;
  }

  public void setEdges(int edges) {
    this.edges = edges;
  }

  public int getNodes() {
    return nodes;
  }

  public void setNodes(int nodes) {
    this.nodes = nodes;
  }
}
