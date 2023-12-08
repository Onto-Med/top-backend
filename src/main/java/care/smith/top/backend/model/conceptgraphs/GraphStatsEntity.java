package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.ConceptGraphStat;

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

  public ConceptGraphStat toApiModel() {
    return new ConceptGraphStat().id(this.getId()).edges(this.getEdges()).nodes(this.getNodes());
  }
}
