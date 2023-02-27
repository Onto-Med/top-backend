package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.DocumentNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;

import java.util.List;

public interface DocumentNodeRepository extends
    Neo4jRepository<DocumentNodeEntity, Long>,
    CypherdslStatementExecutor<DocumentNodeEntity> {

    @Query("MATCH (c:Concept)<-[r1:IN_CONCEPT]-(p:Phrase)<-[r2:HAS_PHRASE]-(d:Document)" +
           "WHERE (c.conceptId IN $conceptIds)" +
//           "AND NOT (p.exemplar XOR $exemplarOnly)" + //ToDo: implement check for whether all or only exemplars should be regarded
           "RETURN DISTINCT d;")
    List<DocumentNodeEntity> getDocumentsForConcepts(List<String> conceptIds, Boolean exemplarOnly);
}
