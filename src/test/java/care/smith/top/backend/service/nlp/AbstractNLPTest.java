package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.nlp.ConceptNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.repository.nlp.PhraseNodeRepository;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NEVER)
public abstract class AbstractNLPTest {//extends AbstractTest {

    @Autowired
    ConceptService conceptService;
    @Autowired
    ConceptNodeRepository conceptRepository;
    @Autowired
    DocumentService documentService;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    DocumentNodeRepository documentNodeRepository;
    @Autowired
    PhraseService phraseService;
    @Autowired
    PhraseNodeRepository phraseRepository;

    protected static Neo4j embeddedNeo4j;
    protected static ElasticsearchClient esClient;
    protected static final String[] ELASTIC_INDEX = new String[] {"test_documents"};
    @Container
    protected static final ElasticsearchContainer elasticsearchContainer = new DocumentElasticsearchContainer();

    private static final Map<String, String> documents =
            Map.of(
                    "test01", "What do we have here? A test document. With an entity. Nice.",
                    "test02", "Another document is here. It has two entities.",
                    "test03", "And a third document; but this one features nothing");

    @BeforeAll
    static void initializeDBs() {
        embeddedNeo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .build();

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
            esClient.index(i -> i
                    .id("01")
                    .index(ELASTIC_INDEX[0])
                    .document( new TextDocument("test01", documents.get("test01")))
            );
            esClient.index(i -> i
                    .id("02")
                    .index(ELASTIC_INDEX[0])
                    .document( new TextDocument("test02", documents.get("test02")))
            );
            esClient.index(i -> i
                    .id("03")
                    .index(ELASTIC_INDEX[0])
                    .document( new TextDocument("test03", documents.get("test03")))
            );
            await().until(() -> esClient.count().count() == 3);
        } catch (IOException e) {
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
