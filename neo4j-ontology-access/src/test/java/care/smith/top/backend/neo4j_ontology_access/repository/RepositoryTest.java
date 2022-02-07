package care.smith.top.backend.neo4j_ontology_access.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataNeo4jTest
@DirtiesContext
public class RepositoryTest {
  private static Neo4j embeddedDatabaseServer;

  @BeforeAll
  static void initializeNeo4j() {
    embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
  }

  @DynamicPropertySource
  static void neo4jProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.neo4j.uri", embeddedDatabaseServer::boltURI);
    registry.add("spring.neo4j.authentication.username", () -> "neo4j");
    registry.add("spring.neo4j.authentication.password", () -> null);
  }

  @AfterAll
  static void stopNeo4j() {
    embeddedDatabaseServer.close();
  }
}
