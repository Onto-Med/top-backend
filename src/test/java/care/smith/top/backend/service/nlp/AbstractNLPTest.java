package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.nlp.ConceptRepository;
import care.smith.top.backend.repository.nlp.DocumentNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.repository.nlp.PhraseRepository;
import care.smith.top.backend.service.AbstractTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Transactional(propagation = Propagation.NEVER)
@Testcontainers
public abstract class AbstractNLPTest extends AbstractTest {
    @Autowired
    ConceptService conceptService;
    @Autowired
    ConceptRepository conceptRepository;
    @Autowired
    DocumentService documentService;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    DocumentNodeRepository documentNodeRepository;
    @Autowired
    PhraseService phraseService;
    @Autowired
    PhraseRepository phraseRepository;

    private static Neo4j embeddedNeo4j;
    private static GenericContainer elasticContainer;
    private static final int ELASTIC_PORT = 9200;
    private static final String ELASTIC_VERSION = "7.17.6";

    @BeforeAll
    static void initializeDBs() {
        embeddedNeo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .build();

        elasticContainer = new GenericContainer(DockerImageName
                .parse("docker.elastic.co/elasticsearch/elasticsearch")
                .withTag(ELASTIC_VERSION));
        elasticContainer.addEnv("discovery.type", "single-node");
        elasticContainer.addEnv("xpack.security.enabled", "false");
        elasticContainer.addExposedPort(ELASTIC_PORT);
        elasticContainer.start();
    }

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", embeddedNeo4j::boltURI);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> null);

        registry.add("spring.elasticsearch.uris", () -> String.format("http://localhost:%s", ELASTIC_PORT));
    }

    @AfterAll
    static void stopDBs() {
        embeddedNeo4j.close();
        elasticContainer.stop();
    }
}
