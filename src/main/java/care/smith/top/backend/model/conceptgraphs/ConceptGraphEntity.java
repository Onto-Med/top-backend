package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphAdjacency;
import care.smith.top.model.ConceptGraphNeighbors;
import care.smith.top.model.ConceptGraphNodes;
import java.util.Arrays;

public class ConceptGraphEntity {
  private AdjacencyObject[] adjacency;
  private NodeObject[] nodes;

  public AdjacencyObject[] getAdjacency() {
    return adjacency;
  }

  public void setAdjacency(AdjacencyObject[] adjacency) {
    this.adjacency = adjacency;
  }

  public NodeObject[] getNodes() {
    return nodes;
  }

  public void setNodes(NodeObject[] nodes) {
    this.nodes = nodes;
  }

  public ConceptGraph toApiModel() {
    ConceptGraph conceptGraph = new ConceptGraph();

    for (AdjacencyObject adj : getAdjacency()) {
      ConceptGraphAdjacency conceptGraphAdjacency = new ConceptGraphAdjacency();
      conceptGraphAdjacency.setId(adj.getId());
      for (Neighbors neighbors : adj.getNeighbors()) {
        conceptGraphAdjacency.addNeighborsItem(
            new ConceptGraphNeighbors()
                .id(neighbors.getId())
                .significance(neighbors.getSignificance())
                .weight(neighbors.getWeight()));
      }
      conceptGraph.addAdjacencyItem(conceptGraphAdjacency);
    }

    for (NodeObject node : getNodes()) {
      conceptGraph.addNodesItem(
          new ConceptGraphNodes()
              .id(node.getId())
              .label(node.getLabel())
              .documents(Arrays.asList(node.getDocuments())));
    }

    return conceptGraph;
  }
}

class AdjacencyObject {
  private String id;
  private Neighbors[] neighbors;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Neighbors[] getNeighbors() {
    return neighbors;
  }

  public void setNeighbors(Neighbors[] neighbors) {
    this.neighbors = neighbors;
  }
}

class NodeObject {
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
}

class Neighbors {
  private String id;
  private Float significance;
  private Float weight;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Float getSignificance() {
    return significance;
  }

  public void setSignificance(Float significance) {
    this.significance = significance;
  }

  public Float getWeight() {
    return weight;
  }

  public void setWeight(Float weight) {
    this.weight = weight;
  }
}
