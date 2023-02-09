package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.DocumentNodeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;

public interface DocumentNodeRepository extends
    Neo4jRepository<DocumentNodeEntity, Long>,
    CypherdslStatementExecutor<DocumentNodeEntity> {

}
