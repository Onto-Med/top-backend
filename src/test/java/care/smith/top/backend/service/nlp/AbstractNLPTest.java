package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import care.smith.top.backend.repository.elasticsearch.DocumentRepository;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

@SpringBootTest
@Transactional(propagation = Propagation.NEVER)
public abstract class AbstractNLPTest {

  protected static final String[] ELASTIC_INDEX = new String[] {"test_documents"};

  @Container
  protected static final ElasticsearchContainer elasticsearchContainer =
      new DocumentElasticsearchContainer();

  private static final Map<String, String> documents =
      Map.of(
          "test01", "What do we have here? A test document. With an entity. Nice.",
          "test02", "Another document is here. It has two entities.",
          "test03", "And a third document; but this one features nothing");

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
  protected static Session neo4jSession;
  protected static ElasticsearchClient esClient;
  @Autowired ConceptClusterService conceptService;
  @Autowired ConceptClusterNodeRepository conceptClusterNodeRepository;
  @Autowired DocumentService documentService;
  @Autowired DocumentRepository documentRepository;
  @Autowired DocumentNodeRepository documentNodeRepository;
  @Autowired PhraseService phraseService;
  @Autowired PhraseNodeRepository phraseRepository;

  @Value("spring.elasticsearch.index.name")
  String elasticIndex;

  @BeforeAll
  static void initializeDBs() {
    setUpNeo4jDB();
    setUpESIndex();
  }

  @DynamicPropertySource
  static void dbProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.neo4j.uri", embeddedNeo4j::boltURI);
    registry.add("spring.neo4j.authentication.username", () -> "neo4j");
    registry.add("spring.neo4j.authentication.password", () -> null);
  }

  @AfterAll
  static void stopDBs() {
    embeddedNeo4j.close();
    esClient.shutdown();
    elasticsearchContainer.stop();
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

  protected static void setUpESIndex() {
    elasticsearchContainer.start();

    RestClient restClient =
        RestClient.builder(HttpHost.create(elasticsearchContainer.getHttpHostAddress())).build();

    ElasticsearchTransport transport =
        new RestClientTransport(restClient, new JacksonJsonpMapper());

    esClient = new ElasticsearchClient(transport);
    assertNotNull(esClient);

    try {
      String elasticIndexName = getIndexName();
      int count = 0;
      for (Map.Entry<String, String> entry : documents.entrySet()) {
        String esId = String.format("%02d", ++count);
        esClient.index(
            i ->
                i.id(esId)
                    .index(elasticIndexName)
                    .document(new TextDocument(entry.getKey(), entry.getValue())));
      }
      await().until(() -> esClient.count().count() == 3);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static String getIndexName() {
    Yaml yaml = new Yaml();
    try {
      Map<String, Object> data =
          yaml.load(
              new FileInputStream(
                  new File(
                      Objects.requireNonNull(
                              elasticsearchContainer
                                  .getClass()
                                  .getClassLoader()
                                  .getResource("application.yml"))
                          .toURI())));
      String elasticTestIndexYaml =
          ((Map<String, String>)
                  ((Map<String, Object>)
                          ((Map<String, Object>) data.get("spring")).get("elasticsearch"))
                      .get("index"))
              .get("name");
      return elasticTestIndexYaml.substring(
          "${DB_ELASTIC_INDEX:".length(), elasticTestIndexYaml.length() - 1);
    } catch (FileNotFoundException | URISyntaxException | ClassCastException e) {
      throw new RuntimeException(e);
    }
  }

  static class TextDocument {
    public String name;
    public String text;

    public TextDocument(String name, String text) {
      this.name = name;
      this.text = text;
    }
  }
}
