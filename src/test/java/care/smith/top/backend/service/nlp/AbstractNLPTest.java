package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import care.smith.top.backend.repository.nlp.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.repository.nlp.PhraseNodeRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NEVER)
public abstract class AbstractNLPTest { // extends AbstractTest {

  @Autowired ConceptService conceptService;
  @Autowired
  ConceptClusterNodeRepository conceptRepository;
  @Autowired DocumentService documentService;
  @Autowired DocumentRepository documentRepository;
  @Autowired DocumentNodeRepository documentNodeRepository;
  @Autowired PhraseService phraseService;
  @Autowired PhraseNodeRepository phraseRepository;

  protected static Neo4j embeddedNeo4j;
  protected static ElasticsearchClient esClient;
  protected static final String[] ELASTIC_INDEX = new String[] {"test_documents"};

  @Container
  protected static final ElasticsearchContainer elasticsearchContainer =
      new DocumentElasticsearchContainer();

  @Value("spring.elasticsearch.index.name")
  String elasticIndex;

  private static final Map<String, String> documents =
      Map.of(
          "test01", "What do we have here? A test document. With an entity. Nice.",
          "test02", "Another document is here. It has two entities.",
          "test03", "And a third document; but this one features nothing");

  @BeforeAll
  static void initializeDBs() {
    embeddedNeo4j = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();

    // use elasticsearch test instance here
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
