package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptClusterNodeRepository
    extends Neo4jRepository<ConceptNodeEntity, String>,
        CypherdslStatementExecutor<ConceptNodeEntity> {

  @Query(
      "OPTIONAL MATCH (n:Concept {conceptId: $conceptId})\n" + "RETURN n IS NOT NULL AS Predicate;")
  Boolean conceptNodeExists(String conceptId);

  @Query(
      "MATCH (d:Document {docId: $documentId})-[:HAS_PHRASE]->(p:Phrase)-[:IN_CONCEPT]->(c:Concept) RETURN c;")
      List<ConceptNodeEntity> getConceptNodesByDocumentId(String documentId);
}
