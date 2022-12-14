package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends
        Neo4jRepository<DocumentEntity, Long>,
        CypherdslStatementExecutor<DocumentEntity> {
}
