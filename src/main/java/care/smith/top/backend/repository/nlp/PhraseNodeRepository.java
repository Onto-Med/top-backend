package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.PhraseNodeEntity;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhraseNodeRepository
    extends Neo4jRepository<PhraseNodeEntity, Long>, CypherdslStatementExecutor<PhraseNodeEntity> {

  @Query(
      "MATCH (d:Document)-[:HAS_PHRASE]-(p:Phrase)"
          + "WHERE (d.docId = $documentId)"
          +
          // "AND NOT (p.exemplar XOR $exemplarOnly)" +
          // ToDo: implement check for whether all or only exemplars should be regarded
          "RETURN DISTINCT p;")
  List<PhraseNodeEntity> getPhrasesForDocument(String documentId, Boolean exemplarOnly);
}
