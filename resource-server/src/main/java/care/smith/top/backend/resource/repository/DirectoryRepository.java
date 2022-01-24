package care.smith.top.backend.resource.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryRepository extends PagingAndSortingRepository<Directory, String> {}
