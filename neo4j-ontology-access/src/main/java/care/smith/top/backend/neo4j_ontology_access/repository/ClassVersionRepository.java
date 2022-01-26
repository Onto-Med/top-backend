package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassVersionRepository extends PagingAndSortingRepository<ClassVersion, Long> {

}
