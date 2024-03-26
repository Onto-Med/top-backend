package care.smith.top.backend.api.nlp;

import care.smith.top.backend.AbstractNLPTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
class ConceptGraphApiDelegateImplTest extends AbstractNLPTest {
  @Autowired ConceptGraphApiDelegateImpl conceptGraphApi;

  @Test
  void getConceptGraphStatistics() {
    assertEquals(
        23,
        Objects.requireNonNull(conceptGraphApi.getConceptGraphStatistics("process_id").getBody()).size());
  }

  @Test
  @Disabled
  void getConceptGraph() {
    // getConceptGraph just calls conceptGraphService.getConceptGraphForIdAndProcess which is already tested
  }

  @Test
  @Disabled
  void getConceptPipelines() {
    // getConceptPipeline just calls conceptGraphService.conceptGraphsService.getAllStoredProcesses which is already tested
  }

  @Test
  @Disabled
  void startConceptGraphPipeline() {
  }
}