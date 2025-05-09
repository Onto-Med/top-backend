package care.smith.top.backend.nlp.api;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.api.nlp.ConceptClusterApiDelegateImpl;
import care.smith.top.backend.nlp.AbstractNLPTest;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConceptClusterApiDelegateImplTest extends AbstractNLPTest {
  private final String exampleDatasource = "exampledatasource";
  private ConceptClusterApiDelegateImpl conceptClusterApi;

  @BeforeEach
  void setUp() throws IOException, InstantiationException {
    ConceptClusterService conceptClusterService =
        new ConceptClusterService(
            conceptGraphsApiService.getAddress().getHostString(),
            conceptClusterNodeRepository,
            phraseRepository,
            documentNodeRepository);
    conceptClusterApi =
        new ConceptClusterApiDelegateImpl(mockedDocumentService(), conceptClusterService);
  }

  @Test
  void getConceptClusters() {
    assertEquals(
        Set.copyOf(concepts2),
        Set.copyOf(
            conceptClusterApi
                .getConceptClusters(
                    List.of("something"), List.of("p1", "p2"), false, exampleDatasource, null, null)
                .getBody()
                .getContent()));
    assertEquals(
        Set.copyOf(concepts1),
        Set.copyOf(
            conceptClusterApi
                .getConceptClusters(
                    List.of("another"), List.of("p1", "p2"), false, exampleDatasource, null, null)
                .getBody()
                .getContent()));
  }

  @Test
  void getConceptClusterById() {
    assertEquals(
        concepts1.get(0),
        conceptClusterApi.getConceptClusterById("c1", exampleDatasource, null).getBody());
    assertEquals(
        concepts2.get(0),
        conceptClusterApi.getConceptClusterById("c2", exampleDatasource, null).getBody());
  }

  @Test
  void getConceptClusterByDocumentId() {
    assertEquals(
        Set.copyOf(concepts1_2),
        Set.copyOf(
            Objects.requireNonNull(
                    conceptClusterApi
                        .getConceptClusterByDocumentId("d2", exampleDatasource, null, 0)
                        .getBody())
                .getContent()));
  }
}
