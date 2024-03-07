package care.smith.top.backend.service.nlp;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import care.smith.top.model.PipelineResponse;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConceptGraphsServiceTest extends AbstractNLPTest {
  @Autowired ConceptGraphsService conceptGraphsService;

  @Test
  void getAllConceptGraphStatistics() {
    Map<String, ConceptGraphStat> conceptGraphStatMap =
        conceptGraphsService.getAllConceptGraphStatistics("process_id");
    assertThat(conceptGraphStatMap).hasSize(23);
  }

  @Test
  void getConceptGraphForIdAndProcess() {
    ConceptGraph conceptGraph =
        conceptGraphsService.getConceptGraphForIdAndProcess("0", "process_id");
    assertThat(conceptGraph).isNotNull().satisfies(c -> {
      assertThat(c.getAdjacency()).hasSize(4);
      assertThat(c.getNodes()).hasSize(4);
    });
  }

  @Test
  void getAllStoredProcesses() {
    List<ConceptGraphProcess> processes = conceptGraphsService.getAllStoredProcesses();
    assertThat(processes).hasSize(2);
  }

  // ToDo: build test zip here to use
  @Test
  @Disabled
  void initPipeline() {
    try {
      PipelineResponse stats =
          conceptGraphsService.initPipeline(
              new File(
                  Objects.requireNonNull(getClass().getClassLoader().getResource("test_files.zip"))
                      .toURI()),
              null,
              null,
              "test",
              null,
              false,
              false);
      System.out.println(stats);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
