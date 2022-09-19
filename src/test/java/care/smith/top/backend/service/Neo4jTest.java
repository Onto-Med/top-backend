package care.smith.top.backend.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@DirtiesContext
public abstract class Neo4jTest {
  static Neo4j embeddedDatabaseServer;

  @BeforeAll
  static void initializeNeo4j() {
    embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
  }

  @BeforeEach
  void cleanup() {
    // workaround to allow non-transactional tests that do not interfere with parallel streams
    embeddedDatabaseServer
        .databaseManagementService()
        .database("neo4j")
        .executeTransactionally("MATCH (n) DETACH DELETE n");
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
