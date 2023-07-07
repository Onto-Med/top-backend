package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.ConceptNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptClusterNodeRepository extends
        Neo4jRepository<ConceptNodeEntity, Long>,
        CypherdslStatementExecutor<ConceptNodeEntity> {
}
