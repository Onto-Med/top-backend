package care.smith.top.backend;

import care.smith.top.backend.configuration.TopBackendContextInitializer;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.util.ResourceHttpHandler;
import care.smith.top.model.Document;
import care.smith.top.top_document_query.adapter.TextAdapter;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(initializers = TopBackendContextInitializer.class)
@Transactional(propagation = Propagation.NEVER)
public abstract class AbstractNLPTest {
  private static final Map<String, Map<String, String>> documentNodes =
      Map.of(
          "d1", Map.of("docId", "d1", "name", "Document 1"),
          "d2", Map.of("docId", "d2", "name", "Document 2"));

  private static final Map<String, Map<String, String>> conceptNodes =
      Map.of(
          "c1", Map.of("conceptId", "c1", "labels", "['phrase','here','another']"),
          "c2", Map.of("conceptId", "c2", "labels", "['something','good']"));

  private static final Map<String, Map<String, String>> phraseNodes =
      Map.of(
          "p1", Map.of("phraseId", "p1", "phrase", "one phrase here", "exemplar", "true"),
          "p2", Map.of("phraseId", "p2", "phrase", "another phrase there", "exemplar", "false"));

  private static final String HAS_PHRASE_REL = "HAS_PHRASE";
  private static final String IN_CONCEPT_REL = "IN_CONCEPT";
  private static final String NEIGHBOR_OF_REL = "NEIGHBOR_OF";

  private static final Map<String, List<Pair<String, String>>> relations =
      Map.of(
          "d1", List.of(Pair.of(HAS_PHRASE_REL, "p2")),
          "d2", List.of(Pair.of(HAS_PHRASE_REL, "p1"), Pair.of(HAS_PHRASE_REL, "p2")),
          "p1", List.of(Pair.of(IN_CONCEPT_REL, "c1"), Pair.of(NEIGHBOR_OF_REL, "p2")),
          "p2", List.of(Pair.of(IN_CONCEPT_REL, "c2"), Pair.of(NEIGHBOR_OF_REL, "p1")));

  protected static Neo4j embeddedNeo4j;
  protected static HttpServer conceptGraphsApiService;
  protected static Session neo4jSession;

  @Autowired protected ConceptClusterService conceptClusterService;
  @Autowired protected ConceptClusterNodeRepository conceptClusterNodeRepository;
  @Autowired protected PhraseNodeRepository phraseRepository;
  @Autowired protected DocumentNodeRepository documentNodeRepository;

  @BeforeAll
  static void setup() throws IOException, InstantiationException {
    setUpNeo4jDB();

    conceptGraphsApiService = HttpServer.create(new InetSocketAddress(9007), 0);
    conceptGraphsApiService.createContext(
        "/processes",
        new ResourceHttpHandler("/concept_graphs_api_fixtures/get_processes.json"));
    conceptGraphsApiService.createContext(
        "/graph/statistics",
        new ResourceHttpHandler("/concept_graphs_api_fixtures/get_statistics.json"));
    conceptGraphsApiService.createContext(
        "/graph/0",
        new ResourceHttpHandler("/concept_graphs_api_fixtures/get_concept_graph.json"));
    conceptGraphsApiService.start();
  }

  @DynamicPropertySource
  static void dbProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.neo4j.uri", embeddedNeo4j::boltURI);
    registry.add("spring.neo4j.authentication.username", () -> "neo4j");
    registry.add("spring.neo4j.authentication.password", () -> null);
  }

  @AfterAll
  static void cleanup() {
    embeddedNeo4j.close();
    conceptGraphsApiService.stop(0);
  }

  protected static void setUpNeo4jDB() {
    embeddedNeo4j = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
    try (Driver driver = GraphDatabase.driver(embeddedNeo4j.boltURI());
        Session session = driver.session()) {
      Map<String, String> typeMap = Map.of("d", "Document", "c", "Concept", "p", "Phrase");
      Map<String, String> idMap = Map.of("d", "documentId", "c", "conceptId", "p", "phraseId");
      neo4jSession = session;
      documentNodes.forEach(
          (key, value) ->
              neo4jSession.run(
                  String.format(
                      "CREATE (:Document {docId: '%s', name: '%s'})",
                      value.get("docId"), value.get("name"))));
      conceptNodes.forEach(
          (key, value) ->
              neo4jSession.run(
                  String.format(
                      "CREATE (:Concept {conceptId: '%s', labels: %s})",
                      value.get("conceptId"), value.get("labels"))));
      phraseNodes.forEach(
          (key, value) ->
              neo4jSession.run(
                  String.format(
                      "CREATE (:Phrase {phraseId: '%s', phrase: '%s', exemplar: %s})",
                      value.get("phraseId"), value.get("phrase"), value.get("exemplar"))));
      relations.forEach(
          (key, list) -> {
            String sId = idMap.get(key.substring(0, 1));
            String sType = typeMap.get(key.substring(0, 1));
            list.forEach(
                pair -> {
                  String tId = idMap.get(pair.getRight().substring(0, 1));
                  String tType = typeMap.get(pair.getRight().substring(0, 1));
                  String rType = pair.getLeft();
                  String query =
                      String.format(
                          "MATCH (s:%s), (t:%s) WHERE s.%s = '%s' AND t.%s = '%s' CREATE (s)-[:%s]->(t)",
                          sType, tType, sId, key, tId, pair.getLeft(), rType);
                  neo4jSession.run(query);
                });
          });
    }
  }
}
