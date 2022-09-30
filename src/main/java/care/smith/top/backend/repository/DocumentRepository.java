package care.smith.top.backend.repository;

import care.smith.top.backend.model.DocumentEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends Neo4jRepository<DocumentEntity, Long> {
}
