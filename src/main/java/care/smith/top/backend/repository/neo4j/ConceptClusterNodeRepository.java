package care.smith.top.backend.repository.neo4j;

import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptClusterNodeRepository
    extends Neo4jRepository<ConceptNodeEntity, Long>,
        CypherdslStatementExecutor<ConceptNodeEntity> {}
