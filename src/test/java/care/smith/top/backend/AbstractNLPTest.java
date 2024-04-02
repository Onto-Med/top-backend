package care.smith.top.backend;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import care.smith.top.backend.extension.Neo4JExtension;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.util.ResourceHttpHandler;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.Document;
import care.smith.top.model.Phrase;
import care.smith.top.top_document_query.adapter.TextAdapter;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractNLPTest {
  protected static Set<Document> documents1 = Set.of(new Document().id("d1").name("Document 1"));
  protected static Set<Document> documents2 = Set.of(new Document().id("d2").name("Document 2"));
  public static Set<Document> documents1_2 =
      Set.of(documents1.iterator().next(), documents2.iterator().next());
  protected static List<ConceptCluster> concepts1 =
      List.of(new ConceptCluster().id("c1").labels(List.of("phrase", "here", "another")));
  protected static List<ConceptCluster> concepts2 =
      List.of(new ConceptCluster().id("c2").labels(List.of("something", "good")));
  public static List<ConceptCluster> concepts1_2 = List.of(concepts1.get(0), concepts2.get(0));
  protected static List<Phrase> phrases1 =
      List.of(new Phrase().id("p1").text("one phrase here").exemplar(true));
  protected static List<Phrase> phrases2 =
      List.of(new Phrase().id("p2").text("another phrase there").exemplar(false));
  public static List<Phrase> phrases1_2 = List.of(phrases1.get(0), phrases2.get(0));
  protected static HttpServer conceptGraphsApiService;
  @RegisterExtension static Neo4JExtension neo4JExtension = new Neo4JExtension();
  @Autowired protected ConceptClusterNodeRepository conceptClusterNodeRepository;
  @Autowired protected PhraseNodeRepository phraseRepository;
  @Autowired protected DocumentNodeRepository documentNodeRepository;

  @DynamicPropertySource
  static void dbProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.neo4j.uri", neo4JExtension.getEmbeddedNeo4j()::boltURI);
    registry.add("spring.neo4j.authentication.username", () -> "neo4j");
    registry.add("spring.neo4j.authentication.password", () -> null);
  }

  @BeforeAll
  static void setup() throws IOException {
    conceptGraphsApiService = HttpServer.create(new InetSocketAddress(9007), 0);
    conceptGraphsApiService.createContext(
        "/processes", new ResourceHttpHandler("/concept_graphs_api_fixtures/get_processes.json"));
    conceptGraphsApiService.createContext(
        "/graph/statistics",
        new ResourceHttpHandler("/concept_graphs_api_fixtures/get_statistics.json"));
    conceptGraphsApiService.createContext(
        "/graph/0", new ResourceHttpHandler("/concept_graphs_api_fixtures/get_concept_graph.json"));
    conceptGraphsApiService.start();
  }

  @AfterAll
  static void cleanup() {
    conceptGraphsApiService.stop(0);
  }

  protected static DocumentService mockedDocumentService()
      throws IOException, InstantiationException {
    PageImpl<Document> page1 = new PageImpl<>(List.of(new Document().id("d1").name("Document 1")));
    PageImpl<Document> page2 = new PageImpl<>(List.of(new Document().id("d2").name("Document 2")));
    PageImpl<Document> page1_2 =
        new PageImpl<>(
            List.of(
                new Document().id("d1").name("Document 1"),
                new Document().id("d2").name("Document 2")));
    DocumentNodeEntity d1 = new DocumentNodeEntity("d1", "Document 1", Set.of());
    DocumentNodeEntity d2 = new DocumentNodeEntity("d2", "Document 2", Set.of());

    TextAdapter adapter = mock(TextAdapter.class);
    when(adapter.getDocumentById("d1"))
        .thenReturn(Optional.ofNullable(new Document().id("d1").name("Document 1")));
    when(adapter.getDocumentById("d2"))
        .thenReturn(Optional.ofNullable(new Document().id("d2").name("Document 2")));
    when(adapter.getDocumentsByIds(eq(Set.of("d1", "d2")), anyInt())).thenReturn(page1_2);
    when(adapter.getDocumentsByIds(eq(Set.of("d1")), anyInt())).thenReturn(page1);
    when(adapter.getDocumentsByIds(eq(Set.of("d2")), anyInt())).thenReturn(page2);
    when(adapter.getAllDocuments(anyInt())).thenReturn(page1_2);
    when(adapter.getDocumentsByName(eq("Document 1"), anyInt())).thenReturn(page1);
    when(adapter.getDocumentsByName(eq("Document 2"), anyInt())).thenReturn(page2);
    when(adapter.getDocumentsByName(eq("Document"), anyInt())).thenReturn(page1_2);

    DocumentNodeRepository documentNodeRepository = mock(DocumentNodeRepository.class);
    when(documentNodeRepository.getDocumentsForPhraseIds(Set.of("p1", "p2"), false))
        .thenReturn(List.of(d1, d2));
    when(documentNodeRepository.getDocumentsForPhraseIds(Set.of("p1", "p2"), true))
        .thenReturn(List.of(d2));
    when(documentNodeRepository.getDocumentsForConceptIds(Set.of("c1", "c2"), false))
        .thenReturn(List.of(d1, d2));
    when(documentNodeRepository.getDocumentsForConceptIds(Set.of("c1"), false))
        .thenReturn(List.of(d2));
    when(documentNodeRepository.getDocumentsForConceptIds(Set.of("c2"), false))
        .thenReturn(List.of(d1, d2));

    DocumentService documentService = mock(DocumentService.class);
    when(documentService.getDocumentNodeRepository()).thenReturn(documentNodeRepository);
    when(documentService.getAdapterFromQuery(anyString(), anyString(), any())).thenReturn(adapter);
    when(documentService.getAdapterForDataSource(anyString())).thenReturn(adapter);
    when(documentService.getDocumentIdsForQuery(anyString(), anyString(), any()))
        .thenReturn(List.of("d1", "d2"));
    when(documentService.getDocumentsForConceptIds(anySet(), anyBoolean())).thenCallRealMethod();
    when(documentService.getDocumentsForConceptIds(anySet(), anyBoolean(), any()))
        .thenCallRealMethod();
    when(documentService.getDocumentsForPhraseIds(anySet(), anyBoolean())).thenCallRealMethod();
    when(documentService.getDocumentsForPhraseTexts(anySet(), anyBoolean())).thenCallRealMethod();

    return documentService;
  }
}
