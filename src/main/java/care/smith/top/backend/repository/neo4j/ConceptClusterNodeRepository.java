package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptClusterNodeRepository
    extends Neo4jRepository<ConceptNodeEntity, Long>,
        CypherdslStatementExecutor<ConceptNodeEntity> {

  @Query(
      "OPTIONAL MATCH (n:Concept {conceptId: $conceptId, corpusId: $corpusId})\n"
          + "RETURN n IS NOT NULL AS Predicate;")
  Boolean conceptNodeExists(String corpusId, String conceptId);

  @Query("MATCH (n:Concept) WITH DISTINCT n.corpusId AS properties RETURN properties;")
  List<String> getCorpusIds();

  @Query(
      "MATCH (c:Concept {corpusId: $corpusId}) CALL { WITH c DETACH DELETE c}"
          + "MATCH (p:Phrase) WHERE NOT (p)-[:IN_CONCEPT]->() CALL { WITH p DETACH DELETE p}"
          + "MATCH (d:Document) WHERE NOT (d)-[:HAS_PHRASE]->() CALL { WITH d DETACH DELETE d}"
          + "MATCH (p:Phrase) WHERE NOT (p)<-[:HAS_PHRASE]-() CALL { WITH p DETACH DELETE p}")
  void removeAllNodesForCorpusId(String corpusId);

  @Query(
      "MATCH (d:Document {docId: $documentId})-[:HAS_PHRASE]->(p:Phrase)-[:IN_CONCEPT]->(c:Concept) RETURN c;")
  List<ConceptNodeEntity> getConceptNodesByDocumentId(String documentId);

  @Query(
      "MATCH (d:Document {docId: $documentId})-[:HAS_PHRASE]->(p:Phrase)-[:IN_CONCEPT]->(c:Concept {corpusId: $corpusId}) RETURN c;")
  List<ConceptNodeEntity> getConceptNodesByDocumentIdAndCorpusId(
      String documentId, String corpusId);

  @Query("UNWIND $labels as l MATCH (c:Concept) WHERE (l in c.labels) RETURN DISTINCT c;")
  List<ConceptNodeEntity> getConceptNodesByLabels(List<String> labels);

  @Query(
      "UNWIND $labels as l MATCH (c:Concept {corpusId: $corpusId}) WHERE (l in c.labels) RETURN DISTINCT c;")
  List<ConceptNodeEntity> getConceptNodesByLabelsAndCorpusId(List<String> labels, String corpusId);

  @Query(
      "UNWIND $phraseIds as pid MATCH (c:Concept)<-[:IN_CONCEPT]-(p:Phrase {phraseId: pid}) RETURN DISTINCT c;")
  List<ConceptNodeEntity> getConceptNodesByPhrases(List<String> phraseIds);

  @Query(
      "UNWIND $phraseIds as pid MATCH (c:Concept {corpusId: $corpusId})<-[:IN_CONCEPT]-(p:Phrase {phraseId: pid}) RETURN DISTINCT c;")
  List<ConceptNodeEntity> getConceptNodesByPhrasesAndCorpusId(
      List<String> phraseIds, String corpusId);
}
