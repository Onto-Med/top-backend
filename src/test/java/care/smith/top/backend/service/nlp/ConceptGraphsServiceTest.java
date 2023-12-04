package care.smith.top.backend.service.nlp;

import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import care.smith.top.model.PipelineResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@SpringBootTest
class ConceptGraphsServiceTest extends AbstractNLPTest{
  @Autowired ConceptGraphsService conceptGraphsService;

  //ToDo: build test zip here to use

  @Test
  void getAllConceptGraphStatistics() {
    Map<String, ConceptGraphStat> conceptGraphStatMap = conceptGraphsService.getAllConceptGraphStatistics("grassco");
  }

  @Test
  void getConceptGraphForIdAndProcess() {
    ConceptGraph conceptGraph = conceptGraphsService.getConceptGraphForIdAndProcess("0", "grassco");
  }

  @Test
  void getStoredProcesses() {
    List<ConceptGraphProcess> processes = conceptGraphsService.getAllStoredProcesses();
  }

  @Test
  void initPipeline() {
    try {
      PipelineResponse stats =
          conceptGraphsService.initPipelineWithBooleans(
              new File(Objects.requireNonNull(getClass().getClassLoader().getResource("test_files.zip")).toURI()),
              "test", false, false);
      System.out.println(stats);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
