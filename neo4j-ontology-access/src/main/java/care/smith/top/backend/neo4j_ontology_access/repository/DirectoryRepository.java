package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.NamedPath;
import org.neo4j.cypherdsl.core.Node;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface DirectoryRepository
    extends PagingAndSortingRepository<Directory, String>,
        CypherdslConditionExecutor<Directory>,
        CypherdslStatementExecutor<Directory> {

  default Collection<Directory> findAllByDescription(String description) {
    return this.findAllByTypeAndNameAndDescription(null, null, description);
  }

  default Collection<Directory> findAllByName(String name) {
    return this.findAllByTypeAndNameAndDescription(null, name, null);
  }

  default Collection<Directory> findAllByType(String type) {
    return this.findAllByTypeAndNameAndDescription(type, null, null);
  }

  default Collection<Directory> findAllByTypeAndNameAndDescription(
      String type, String name, String description) {
    Node d = Cypher.node("Directory").named("d");
    NamedPath p =
        Cypher.path("p").definedBy(d.relationshipTo(Cypher.node("Directory"), "BELONGS_TO"));

    return this.findAll(
        Cypher.match(d)
            .where(type != null ? d.hasLabels(type) : Cypher.literalTrue().asCondition())
            .and(
                name != null
                    ? Functions.toLower(d.property("name"))
                        .contains(Cypher.anonParameter(name.toLowerCase()))
                    : Cypher.literalTrue().asCondition())
            .and(
                description != null
                    ? Functions.toLower(d.property("description"))
                        .contains(Cypher.anonParameter(description.toLowerCase()))
                    : Cypher.literalTrue().asCondition())
            .optionalMatch(p)
            .returning(
                d.getRequiredSymbolicName(),
                Functions.collect(Functions.nodes(p)),
                Functions.collect(Functions.relationships(p)))
            .build());
  }
}
