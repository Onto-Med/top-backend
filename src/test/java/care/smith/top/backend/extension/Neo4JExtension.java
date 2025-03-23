package care.smith.top.backend.extension;

import static care.smith.top.backend.AbstractNLPTest.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

public class Neo4JExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
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
  private static boolean started = false;
  private static Neo4j embeddedNeo4j;
  private static Session neo4jSession;

  private static void setUpNeo4jDB() {
    embeddedNeo4j = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
    try (Driver driver = GraphDatabase.driver(embeddedNeo4j.boltURI());
        Session session = driver.session()) {
      Map<String, String> typeMap = Map.of("d", "Document", "c", "Concept", "p", "Phrase");
      Map<String, String> idMap = Map.of("d", "docId", "c", "conceptId", "p", "phraseId");
      neo4jSession = session;
      documents1_2.forEach(
          document ->
              neo4jSession.run(
                  String.format(
                      "CREATE (:Document {docId: '%s', name: '%s', corpusId: '%s'})",
                      document.getId(), document.getName(), "exampleDatasource")));
      concepts1_2.forEach(
          concept -> {
            String labels =
                concept.getLabels().stream()
                    .map(s -> String.format("'%s'", s.trim()))
                    .collect(Collectors.joining(","));
            neo4jSession.run(
                String.format(
                    "CREATE (:Concept {conceptId: '%s', labels: %s, corpusId: '%s'})",
                    concept.getId(), String.format("[%s]", labels), "exampleDatasource"));
          });
      phrases1_2.forEach(
          phrase ->
              neo4jSession.run(
                  String.format(
                      "CREATE (:Phrase {phraseId: '%s', phrase: '%s', exemplar: %s, corpusId: '%s'})",
                      phrase.getId(), phrase.getText(), phrase.isExemplar(), "exampleDatasource")));

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
                          "MATCH (s:%s), (t:%s) WHERE s.%s = '%s' AND t.%s = '%s' CREATE (s)-[:%s%s]->(t)",
                          sType,
                          tType,
                          sId,
                          key,
                          tId,
                          triple.getRight(),
                          rType,
                          rParam != null ? rParam : "");
                  neo4jSession.run(query);
                });
          });
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!started) {
      started = true;
      setUpNeo4jDB();
      context
          .getRoot()
          .getStore(ExtensionContext.Namespace.GLOBAL)
          .put("test-data-setup-started", this);
    }
  }

  @Override
  public void close() {
    embeddedNeo4j.close();
  }

  public URI getBoltUri() {
    return embeddedNeo4j.boltURI();
  }
}
