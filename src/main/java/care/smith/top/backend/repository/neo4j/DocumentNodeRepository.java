package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentNodeRepository
    extends Neo4jRepository<DocumentNodeEntity, String>,
        CypherdslStatementExecutor<DocumentNodeEntity> {

  @Query(
      "MATCH (c:Concept {corpusId: $corpusId})<-[:IN_CONCEPT]-(p:Phrase)<-[:HAS_PHRASE]-(d:Document)\n"
          + "WITH d, p,\n"
          + "CASE $exemplarOnly\n"
          + "  WHEN true  THEN (p.exemplar AND $exemplarOnly)\n"
          + "  WHEN false THEN true\n"
          + "END AS returnBool\n"
          + "WHERE (c.conceptId IN $conceptIds)\n"
          + "  AND returnBool\n"
          + "RETURN DISTINCT d;")
  List<DocumentNodeEntity> getDocumentsForConceptIds(
      Set<String> conceptIds, String corpusId, Boolean exemplarOnly);

  @Query(
      "MATCH (d:Document)-[:HAS_PHRASE]->(p:Phrase)-[:IN_CONCEPT]->(c:Concept {corpusId: $corpusId})\n"
          + "WITH d, p,\n"
          + "CASE $exemplarOnly\n"
          + "  WHEN true  THEN (p.exemplar AND $exemplarOnly)\n"
          + "  WHEN false THEN true\n"
          + "END AS returnBool\n"
          + "WHERE (p.phraseId IN $phraseIds)\n"
          + "  AND returnBool\n"
          + "RETURN DISTINCT d;")
  List<DocumentNodeEntity> getDocumentsForPhraseIds(
      Set<String> phraseIds, String corpusId, Boolean exemplarOnly);

  @Query(
      "UNWIND $phraseTexts as labels\n"
          + "MATCH (d:Document)-[:HAS_PHRASE]->(p:Phrase)-[:IN_CONCEPT]->(c:Concept {corpusId: $corpusId})\n"
          + "WITH d, p,\n"
          + "CASE $exemplarOnly\n"
          + "  WHEN true  THEN (p.exemplar AND $exemplarOnly)\n"
          + "  WHEN false THEN true\n"
          + "END AS returnBool\n"
          + "WHERE (p.phrase CONTAINS labels)\n"
          + "  AND returnBool\n"
          + "RETURN DISTINCT d;")
  List<DocumentNodeEntity> getDocumentsForPhrasesText(
      Set<String> phraseTexts, String corpusId, Boolean exemplarOnly);

  @Query(
      "OPTIONAL MATCH (n:Document {docId: $documentId})\n" + "RETURN n IS NOT NULL AS Predicate;")
  Boolean documentNodeExists(String documentId);
}
