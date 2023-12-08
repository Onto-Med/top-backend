package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.PipelineResponse;

public class PipelineFailEntity implements PipelineResponseEntity {
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public PipelineResponse getSpecificResponse() {
    return new PipelineResponse().name(this.getName()).response("Pipeline failed. Check the logs.");
  }
}
