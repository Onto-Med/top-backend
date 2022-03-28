package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Ontology;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OntologyRepository
    extends PagingAndSortingRepository<Ontology, String>,
        CypherdslConditionExecutor<Ontology>,
        CypherdslStatementExecutor<Ontology> {

  @Query(
      "MATCH (o:Ontology { id: $ontologyId }) -[:BELONGS_TO]-> (:Repository { id: $repositoryId }) "
          + "RETURN o")
  Optional<Ontology> findByIdAndRepositoryId(
      @Param("ontologyId") String ontologyId, @Param("repositoryId") String repositoryId);
}
