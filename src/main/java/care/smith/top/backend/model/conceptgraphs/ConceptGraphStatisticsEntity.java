package care.smith.top.backend.model.conceptgraphs;

import care.smith.top.model.PipelineResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;

public class ConceptGraphStatisticsEntity implements PipelineResponseEntity {
  private String name;
  private int numberOfGraphs;
  private GraphStatsEntity[] conceptGraphs;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

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

  @Override
  public PipelineResponse getSpecificResponse() {
    //ToDo: catch null conceptGraphs/numberOfGraphs
    String status = "";
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      status = mapper.writeValueAsString(
          Map.of(
              "conceptGraphs", conceptGraphs,
              "numberOfGraphs", numberOfGraphs
          )
      );
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return new PipelineResponse()
        .name(this.getName())
        .response(status);
  }
}