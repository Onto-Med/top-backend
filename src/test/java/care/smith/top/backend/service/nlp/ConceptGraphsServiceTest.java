package care.smith.top.backend.service.nlp;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphPipeline;
import care.smith.top.model.ConceptGraphStat;
import java.util.List;
import java.util.Map;
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
    List<ConceptGraphPipeline> processes = conceptGraphsService.getAllStoredProcesses();
    assertThat(processes).hasSize(2);
  }

  @Test
  void count() {
    assertThat(conceptGraphsService.count()).isEqualTo(2);
  }
}
