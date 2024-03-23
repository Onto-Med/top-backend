package care.smith.top.backend.extension;

import care.smith.top.model.ConceptCluster;
import care.smith.top.model.Document;
import care.smith.top.model.Phrase;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import static care.smith.top.backend.AbstractNLPTest.*;

public class Neo4JExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
  private static final String HAS_PHRASE_REL = "HAS_PHRASE";
  private static final String IN_CONCEPT_REL = "IN_CONCEPT";
  private static final String NEIGHBOR_OF_REL = "NEIGHBOR_OF";
  private static final Map<String, List<Pair<String, String>>> relations =
      Map.of(
          "d1", List.of(Pair.of(HAS_PHRASE_REL, "p2")),
          "d2", List.of(Pair.of(HAS_PHRASE_REL, "p1"), Pair.of(HAS_PHRASE_REL, "p2")),
          "p1", List.of(Pair.of(IN_CONCEPT_REL, "c1"), Pair.of(NEIGHBOR_OF_REL, "p2")),
          "p2", List.of(Pair.of(IN_CONCEPT_REL, "c2"), Pair.of(NEIGHBOR_OF_REL, "p1")));
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
                      "CREATE (:Document {docId: '%s', name: '%s'})",
                      document.getId(), document.getName())));
      concepts1_2.forEach(
          concept -> {
            String labels =
                concept.getLabels().stream()
                    .map(s -> String.format("'%s'", s.trim()))
                    .collect(Collectors.joining(","));
            neo4jSession.run(
                String.format(
                    "CREATE (:Concept {conceptId: '%s', labels: %s})",
                    concept.getId(), String.format("[%s]", labels)));
          });
      phrases1_2.forEach(
          phrase ->
              neo4jSession.run(
                  String.format(
                      "CREATE (:Phrase {phraseId: '%s', phrase: '%s', exemplar: %s})",
                      phrase.getId(), phrase.getText(), phrase.isExemplar())));

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
                          sType, tType, sId, key, tId, pair.getRight(), rType);
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

  public Neo4j getEmbeddedNeo4j() {
    return embeddedNeo4j;
  }
}
