package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.nlp.ConceptNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.repository.nlp.PhraseNodeRepository;
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

@Transactional(propagation = Propagation.NEVER)
public abstract class AbstractNLPTest extends AbstractTest {

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

    private static Neo4j embeddedNeo4j;

    @BeforeAll
    static void initializeDBs() {
        embeddedNeo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .build();

        // use elasticsearch test instance here
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
    }
}
