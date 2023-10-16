package care.smith.top.backend.model.conceptgraphs;

public class ConceptGraphStatisticsEntity {
  private int numberOfGraphs;
  private GraphStatsEntity[] conceptGraphs;

  public int getNumberOfGraphs() {
    return numberOfGraphs;
  }

  public void setNumberOfGraphs(int numberOfGraphs) {
    this.numberOfGraphs = numberOfGraphs;
  }

  public GraphStatsEntity[] getConceptGraphs() {
    return conceptGraphs;
  }

  public void setConceptGraphs(GraphStatsEntity[] conceptGraphs) {
    this.conceptGraphs = conceptGraphs;
  }
}
