package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.ConceptNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptNodeRepository extends
        Neo4jRepository<ConceptNodeEntity, Long>,
        CypherdslStatementExecutor<ConceptNodeEntity> {
}
