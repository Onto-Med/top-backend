package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.PhraseNodeEntity;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhraseNodeRepository
    extends Neo4jRepository<PhraseNodeEntity, Long>, CypherdslStatementExecutor<PhraseNodeEntity> {

  @Query(
      "MATCH (d:Document)-[:HAS_PHRASE]-(p:Phrase)-[:IN_CONCEPT]->(c:Concept {corpusId: $corpusId})"
          + "WHERE (d.docId = $documentId)"
          + "AND (NOT $exemplarOnly OR NOT (p.exemplar XOR $exemplarOnly))"
          + "RETURN DISTINCT p;")
  List<PhraseNodeEntity> getPhrasesForDocument(
      String documentId, String corpusIds, Boolean exemplarOnly);

  @Query(
      "OPTIONAL MATCH (n:Phrase {phraseId: $phraseId})-[:IN_CONCEPT]->(c:Concept {corpusId: $corpusId})"
          + "RETURN n IS NOT NULL AS Predicate;")
  Boolean phraseNodeExists(String phraseId, String corpusIds);

  @Query(
      "MATCH (n:Phrase WHERE n.phraseId IN $phraseIds)-[:IN_CONCEPT]->(c:Concept {corpusId: $corpusId}) RETURN DISTINCT n")
  List<PhraseNodeEntity> getPhrasesForIds(List<String> phraseIds, String corpusIds);
}
