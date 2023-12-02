package care.smith.top.backend.model.conceptgraphs;

public class PipelineStatusEntity implements PipelineResponseEntity {
  private String name;
  private PipelineStatus status;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PipelineStatus getStatus() {
    return status;
  }

  public void setStatus(PipelineStatus status) {
    this.status = status;
  }
}

class PipelineStatus {
  private String dataStatus;
  private String embeddingStatus;
  private String clusteringStatus;
  private String graphStatus;

  public String getDataStatus() {
    return dataStatus;
  }

  public void setDataStatus(String dataStatus) {
    this.dataStatus = dataStatus;
  }

  public String getEmbeddingStatus() {
    return embeddingStatus;
  }

  public void setEmbeddingStatus(String embeddingStatus) {
    this.embeddingStatus = embeddingStatus;
  }

  public String getClusteringStatus() {
    return clusteringStatus;
  }

  public void setClusteringStatus(String clusteringStatus) {
    this.clusteringStatus = clusteringStatus;
  }

  public String getGraphStatus() {
    return graphStatus;
  }

  public void setGraphStatus(String graphStatus) {
    this.graphStatus = graphStatus;
  }
}