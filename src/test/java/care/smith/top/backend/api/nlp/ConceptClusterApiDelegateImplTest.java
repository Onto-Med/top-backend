package care.smith.top.backend.api.nlp;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.DocumentService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConceptClusterApiDelegateImplTest extends AbstractNLPTest {

  private ConceptClusterApiDelegateImpl conceptClusterApi;

  @BeforeEach
  void setUp() throws IOException, InstantiationException {
    ConceptClusterService conceptClusterService = new ConceptClusterService(conceptGraphsApiService.getAddress().getHostString(), conceptClusterNodeRepository, phraseRepository, documentNodeRepository);
    conceptClusterApi = new ConceptClusterApiDelegateImpl(mockedDocumentService(), conceptClusterService);
  }

  @Test
  void getConceptClusters() {
    assertEquals(
        Set.copyOf(concepts2),
        Set.copyOf(conceptClusterApi.getConceptClusters(
            List.of("something"), List.of("p1", "p2"), false, null, null,null).getBody().getContent()));
    assertEquals(
        Set.copyOf(concepts1),
        Set.copyOf(conceptClusterApi.getConceptClusters(
            List.of("another"), List.of("p1", "p2"), false, null, null, null).getBody().getContent()));

  }

  @Test
  void getConceptClusterById() {
    assertEquals(
        concepts1.get(0),
        conceptClusterApi.getConceptClusterById("c1", null, null).getBody());
    assertEquals(
        concepts2.get(0),
        conceptClusterApi.getConceptClusterById("c2", null, null).getBody());
  }

  @Test
  void getConceptClusterByDocumentId() {
    assertEquals(
        Set.copyOf(concepts1_2),
        Set.copyOf(Objects.requireNonNull(conceptClusterApi.getConceptClusterByDocumentId(
            "d2", "ExampleDatasource", null, 0).getBody()).getContent())
    );
  }

  @Test
  @Disabled
  void createConceptClustersForPipelineId() {
  }
}