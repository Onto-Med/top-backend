package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.DocumentNodeEntity;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;

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
}
