package care.smith.top.backend.api.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.AbstractNLPTest;
import java.util.Objects;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConceptPipelineApiDelegateImplTest extends AbstractNLPTest {
  @Autowired ConceptPipelineApiDelegateImpl conceptGraphApi;

  @Test
  void getConceptGraphStatistics() {
    assertEquals(
        23,
        Objects.requireNonNull(conceptGraphApi.getConceptGraphStatistics("process_id").getBody())
            .size());
  }

  @Test
  @Disabled
  void getConceptGraph() {
    // getConceptGraph just calls conceptGraphService.getConceptGraphForIdAndProcess which is
    // already tested
  }

  @Test
  @Disabled
  void getConceptPipelines() {
    // getConceptPipeline just calls conceptGraphService.conceptGraphsService.getAllStoredProcesses
    // which is already tested
  }

  @Test
  @Disabled
  void deleteConceptPipelineById() {}

  @Test
  @Disabled
  void getConceptPipelineById() {}

  @Test
  @Disabled
  void startConceptGraphPipeline() {}
}
