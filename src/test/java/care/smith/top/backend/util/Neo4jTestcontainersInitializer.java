package care.smith.top.backend.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Neo4jContainer;

public class Neo4jTestcontainersInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>, AfterAllCallback {
  private static final String HAS_PHRASE_REL = "HAS_PHRASE";
  private static final String IN_CONCEPT_REL = "IN_CONCEPT";
  private static final String NEIGHBOR_OF_REL = "NEIGHBOR_OF";
  private static final Map<String, List<Triple<String, String, String>>> relations =
      Map.of(
          "d1", List.of(Triple.of(HAS_PHRASE_REL, " {begin: 0, end: 5}", "p2")),
          "d2",
              List.of(
                  Triple.of(HAS_PHRASE_REL, " {begin: 6, end: 10}", "p1"),
                  Triple.of(HAS_PHRASE_REL, " {begin: 11, end: 15}", "p2")),
          "p1",
              List.of(
                  Triple.of(IN_CONCEPT_REL, null, "c1"), Triple.of(NEIGHBOR_OF_REL, null, "p2")),
          "p2",
              List.of(
                  Triple.of(IN_CONCEPT_REL, null, "c2"), Triple.of(NEIGHBOR_OF_REL, null, "p1")));
  private static final String exampleDatasource = "exampledatasource";

  Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5");

  @Override
  public void afterAll(ExtensionContext context) {
    if (neo4j != null) neo4j.stop();
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    neo4j.start();

    TestPropertyValues.of(
            "spring.neo4j.uri=" + neo4j.getBoltUrl(),
            "spring.neo4j.authentication.username=" + "neo4j",
            "spring.neo4j.authentication.password=" + neo4j.getAdminPassword())
        .applyTo(applicationContext.getEnvironment());

    setUpNeo4jDb();
  }

  private void setUpNeo4jDb() {
    AuthToken authToken = AuthTokens.basic("neo4j", neo4j.getAdminPassword());
    try (Driver driver = GraphDatabase.driver(neo4j.getBoltUrl(), authToken);
        Session session = driver.session()) {
      Map<String, String> typeMap = Map.of("d", "Document", "c", "Concept", "p", "Phrase");
      Map<String, String> idMap = Map.of("d", "docId", "c", "conceptId", "p", "phraseId");
      AbstractNLPTest.documents1_2.forEach(
          document ->
              session.run(
                  String.format(
                      "CREATE (:Document {docId: '%s', name: '%s'})",
                      document.getId(), document.getName())));
      AbstractNLPTest.concepts1_2.forEach(
          concept -> {
            String labels =
                concept.getLabels().stream()
                    .map(s -> String.format("'%s'", s.trim()))
                    .collect(Collectors.joining(","));
            session.run(
                String.format(
                    "CREATE (:Concept {conceptId: '%s', labels: %s, corpusId: '%s'})",
                    concept.getId(), String.format("[%s]", labels), exampleDatasource));
          });
      AbstractNLPTest.phrases1_2.forEach(
          phrase ->
              session.run(
                  String.format(
                      "CREATE (:Phrase {phraseId: '%s', phrase: '%s', exemplar: %s})",
                      phrase.getId(), phrase.getText(), phrase.isExemplar())));

      relations.forEach(
          (key, list) -> {
            String sId = idMap.get(key.substring(0, 1));
            String sType = typeMap.get(key.substring(0, 1));
            list.forEach(
                triple -> {
                  String tId = idMap.get(triple.getRight().substring(0, 1));
                  String tType = typeMap.get(triple.getRight().substring(0, 1));
                  String rType = triple.getLeft();
                  String rParam = triple.getMiddle();
                  String query =
                      String.format(
                          "MATCH (s:%s), (t:%s) WHERE s.%s = '%s' AND t.%s = '%s' CREATE"
                              + " (s)-[:%s%s]->(t)",
                          sType,
                          tType,
                          sId,
                          key,
                          tId,
                          triple.getRight(),
                          rType,
                          rParam != null ? rParam : "");
                  session.run(query);
                });
          });
    }
  }
}
