package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentNodeRepository
    extends Neo4jRepository<DocumentNodeEntity, Long>,
        CypherdslStatementExecutor<DocumentNodeEntity> {

  @Query(
      "MATCH (c:Concept)<-[r1:IN_CONCEPT]-(p:Phrase)<-[r2:HAS_PHRASE]-(d:Document)\n"
          + "WITH d, p,\n"
          + "CASE $exemplarOnly\n"
          + "  WHEN true  THEN (p.exemplar AND $exemplarOnly)\n"
          + "  WHEN false THEN true\n"
          + "END AS returnBool\n"
          + "WHERE (c.conceptId IN $conceptIds)\n"
          + "AND returnBool\n"
          + "RETURN DISTINCT d;")
  List<DocumentNodeEntity> getDocumentsForConcepts(List<String> conceptIds, Boolean exemplarOnly);

  @Query(
      "OPTIONAL MATCH (n:Document {documentId: $documentId})\n" +
          "RETURN n IS NOT NULL AS Predicate;"
  )
  Boolean documentNodeExists(String documentId);
}
