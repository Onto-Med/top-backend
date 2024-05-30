package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.model.ConceptCluster;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConceptClusterServiceTest extends AbstractNLPTest {
  @Autowired protected ConceptClusterService conceptClusterService;

  @Test
  void count() {
    assertEquals(Long.valueOf(2), conceptClusterService.count());
  }

  @Test
  void concepts() {
    assertEquals(
        Set.copyOf(concepts1_2),
        conceptClusterService.concepts(null).stream().collect(Collectors.toSet()));
  }

  @Test
  void conceptsForPage() {
    assertEquals(
        conceptClusterService.concepts(null).stream().collect(Collectors.toSet()),
        conceptClusterService.conceptsForPage(null, null).stream().collect(Collectors.toSet()));
  }

  @Test
  void conceptById() {
    ConceptCluster response1 = conceptClusterService.conceptById("c1", null);
    assertEquals(concepts1.get(0), response1);

    ConceptCluster response2 = conceptClusterService.conceptById("c2", null);
    assertEquals(concepts2.get(0), response2);
  }

  @Test
  void conceptsByDocumentId() {
    assertEquals(
        Set.copyOf(concepts1_2),
        new HashSet<>(conceptClusterService.conceptsByDocumentId("d2", null, 0).getContent()));
  }

  @Test
  void conceptsByLabels() {
    assertEquals(
        Set.copyOf(concepts1),
        new HashSet<>(
            conceptClusterService.conceptsByLabels(List.of("phrase"), null, 0).getContent()));
    assertEquals(
        Set.copyOf(concepts1_2),
        new HashSet<>(
            conceptClusterService
                .conceptsByLabels(List.of("phrase", "good"), null, 0)
                .getContent()));
  }

  @Test
  void conceptsByPhraseIds() {
    assertEquals(
        Set.copyOf(concepts1),
        new HashSet<>(
            conceptClusterService.conceptsByPhraseIds(List.of("p1"), null, 0).getContent()));
    assertEquals(
        Set.copyOf(concepts1_2),
        new HashSet<>(
            conceptClusterService.conceptsByPhraseIds(List.of("p1", "p2"), null, 0).getContent()));
  }

  @Test
  void conceptsByLabelsAndPhrases() {
    assertEquals(
        Set.copyOf(concepts2),
        new HashSet<>(
            conceptClusterService
                .conceptsByLabelsAndPhrases(List.of("good"), List.of("p1", "p2"), null, 0)
                .getContent()));
  }
}
