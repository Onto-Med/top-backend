package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.model.ConceptCluster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class ConceptClusterServiceTest extends AbstractNLPTest {
  @Autowired
  protected ConceptClusterService conceptClusterService;
  @Test
  void conceptById() {
    ConceptCluster response1 = conceptClusterService.conceptById("c1");
    assertEquals(concepts1.get(0), response1);

    ConceptCluster response2 = conceptClusterService.conceptById("c2");
    assertEquals(concepts2.get(0), response2);
  }

  @Test
  void concepts() {
    assertEquals(
        Set.copyOf(concepts1_2), conceptClusterService.concepts().stream().collect(Collectors.toSet()));
  }

  @Test
  void conceptsByDocumentId() {
    assertEquals(Set.copyOf(concepts1_2), new HashSet<>(conceptClusterService.conceptsByDocumentId("d2", 0).getContent()));
  }

  @Test
  void count() {
    assertEquals(Long.valueOf(2), conceptClusterService.count());
  }
}
