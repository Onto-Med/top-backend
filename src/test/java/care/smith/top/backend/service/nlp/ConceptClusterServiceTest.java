package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.model.ConceptCluster;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ConceptClusterServiceTest extends AbstractNLPTest {

  @Test
  @Disabled
  void conceptById() {
    // see AbstractNLPTest for creation of the Neo4J DB entries
    ConceptCluster cluster1 = conceptService.conceptById("c1");
    assertEquals("c1", cluster1.getId());
    assertEquals("phrase, here, another", cluster1.getLabels());

    ConceptCluster cluster2 = conceptService.conceptById("c2");
    assertEquals("c2", cluster2.getId());
    assertEquals("something, good", cluster2.getLabels());
  }
}
