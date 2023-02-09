package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.PhraseEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PhraseRepository extends
        Neo4jRepository<PhraseEntity, Long>,
        CypherdslStatementExecutor<PhraseEntity> {

    @Query("MATCH (n:Phrase) \n" +
            "WITH COLLECT(DISTINCT labels(n)) AS nested\n" +
            "UNWIND nested as x\n" +
            "UNWIND x as y\n" +
            "RETURN COLLECT(DISTINCT y);")
    Object getConceptCollection();
}

