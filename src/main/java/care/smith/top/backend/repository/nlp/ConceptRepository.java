package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.ConceptEntity;
import care.smith.top.backend.model.nlp.PhraseEntity;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptRepository extends
        Neo4jRepository<ConceptEntity, Long>,
        CypherdslStatementExecutor<ConceptEntity> {

    @Query("MATCH (p: Phrase)-[r:IN_CONCEPT]-(c: Concept {conceptId: $conceptId}) WHERE p.exemplar RETURN p;")
    List<PhraseEntity> phrasesForConcept(String conceptId);
}
