package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClassRepository extends PagingAndSortingRepository<Class, UUID> {
}
