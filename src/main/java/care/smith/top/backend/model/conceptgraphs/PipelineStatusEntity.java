package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.PipelineResponse;
import care.smith.top.model.PipelineResponseStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

  @Override
  public PipelineResponse getSpecificResponse() {
    return new PipelineResponse().name(this.getName()).response(this.getStatus().toJsonString()).status(PipelineResponseStatus.SUCCESSFUL);
  }
}

class PipelineStatus {
  @JsonProperty("data")
  private String dataStatus;

  @JsonProperty("embedding")
  private String embeddingStatus;

  @JsonProperty("clustering")
  private String clusteringStatus;

  @JsonProperty("graph")
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

  public String toJsonString() {
    String status = "";
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      status = mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return status;
  }
}
