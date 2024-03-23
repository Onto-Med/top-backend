package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.model.ConceptCluster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ConceptClusterServiceTest extends AbstractNLPTest {
  @Autowired
  protected ConceptClusterService conceptClusterService;
  @Test
  void count() {
    assertEquals(Long.valueOf(2), conceptClusterService.count());
  }

  @Test
  void concepts() {
    assertEquals(
        Set.copyOf(concepts1_2), conceptClusterService.concepts().stream().collect(Collectors.toSet()));
  }

  @Test
  void conceptsForPage() {
    assertEquals(
        conceptClusterService.concepts().stream().collect(Collectors.toSet()),
        conceptClusterService.conceptsForPage(null).stream().collect(Collectors.toSet())
    );
  }

  @Test
  void conceptById() {
    ConceptCluster response1 = conceptClusterService.conceptById("c1");
    assertEquals(concepts1.get(0), response1);

    ConceptCluster response2 = conceptClusterService.conceptById("c2");
    assertEquals(concepts2.get(0), response2);
  }

  @Test
  void conceptsByDocumentId() {
    assertEquals(
        Set.copyOf(concepts1_2),
        new HashSet<>(conceptClusterService.conceptsByDocumentId("d2", 0).getContent()));
  }

  @Test
  void conceptsByLabels() {
    assertEquals(
        Set.copyOf(concepts1),
        new HashSet<>(conceptClusterService.conceptsByLabels(List.of("phrase"), 0).getContent()));
    assertEquals(
        Set.copyOf(concepts1_2),
        new HashSet<>(conceptClusterService.conceptsByLabels(List.of("phrase", "good"), 0).getContent()));
  }

  @Test
  void conceptsByPhraseIds() {
    assertEquals(
        Set.copyOf(concepts1),
        new HashSet<>(conceptClusterService.conceptsByPhraseIds(List.of("p1"), 0).getContent()));
    assertEquals(
        Set.copyOf(concepts1_2),
        new HashSet<>(conceptClusterService.conceptsByPhraseIds(List.of("p1", "p2"), 0).getContent()));
  }

  @Test
  void conceptsByLabelsAndPhrases() {
    assertEquals(
        Set.copyOf(concepts2),
        new HashSet<>(conceptClusterService.conceptsByLabelsAndPhrases(List.of("good"), List.of("p1", "p2"), 0).getContent()));
  }
}
