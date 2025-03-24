package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.HasPhraseRelationship;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhraseDocumentRelationRepository
    extends Neo4jRepository<HasPhraseRelationship, Long>,
        CypherdslStatementExecutor<HasPhraseRelationship> {
  @Query(
      "MATCH (:Document {docId: $documentId})-[r:HAS_PHRASE]->(p:Phrase)-[:IN_CONCEPT]->(:Concept {conceptId: $conceptId, corpusId: $corpusId}) RETURN properties(r)")
  List<HasPhraseRelationship> getPhraseRelationshipsByDocumentAndConcept(
      String documentId, String corpusId, String conceptId);
}
