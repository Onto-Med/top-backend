package care.smith.top.backend.model.conceptgraphs.pipelineresponses;

import care.smith.top.model.PipelineResponse;
import care.smith.top.model.PipelineResponseStatus;

public class PipelineFailWithExplicit extends PipelineFailEntity {
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public PipelineResponse getSpecificResponse() {
    return new PipelineResponse()
        .name(this.getName())
        .response(this.getStatus())
        .status(PipelineResponseStatus.FAILED);
  }
}
