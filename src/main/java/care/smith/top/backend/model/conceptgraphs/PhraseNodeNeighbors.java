package care.smith.top.backend.model.conceptgraphs;

public class PhraseNodeNeighbors {
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
