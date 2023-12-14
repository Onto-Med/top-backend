package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphAdjacency;
import care.smith.top.model.ConceptGraphNeighbors;
import care.smith.top.model.ConceptGraphNodes;
import java.util.Arrays;

public class ConceptGraphEntity {
  private AdjacencyObject[] adjacency;
  private PhraseNodeObject[] nodes;

  public AdjacencyObject[] getAdjacency() {
    return adjacency;
  }

  public void setAdjacency(AdjacencyObject[] adjacency) {
    this.adjacency = adjacency;
  }

  public PhraseNodeObject[] getNodes() {
    return nodes;
  }

  public void setNodes(PhraseNodeObject[] nodes) {
    this.nodes = nodes;
  }

  public ConceptGraph toApiModel() {
    ConceptGraph conceptGraph = new ConceptGraph();

    for (AdjacencyObject adj : getAdjacency()) {
      ConceptGraphAdjacency conceptGraphAdjacency = new ConceptGraphAdjacency();
      conceptGraphAdjacency.setId(adj.getId());
      for (PhraseNodeNeighbors neighbors : adj.getNeighbors()) {
        conceptGraphAdjacency.addNeighborsItem(
            new ConceptGraphNeighbors()
                .id(neighbors.getId())
                .significance(neighbors.getSignificance())
                .weight(neighbors.getWeight()));
      }
      conceptGraph.addAdjacencyItem(conceptGraphAdjacency);
    }

    for (PhraseNodeObject node : getNodes()) {
      conceptGraph.addNodesItem(
          new ConceptGraphNodes()
              .id(node.getId())
              .label(node.getLabel())
              .documents(Arrays.asList(node.getDocuments())));
    }

    return conceptGraph;
  }
}

