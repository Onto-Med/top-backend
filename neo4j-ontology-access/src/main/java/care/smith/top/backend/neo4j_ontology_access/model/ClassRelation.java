package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * This class describes relations between classes (super-sub-class relations). Relations always
 * belong to a repository or ontology. If there is no superclass, the subclass is a root node.
 */
@Node
public class ClassRelation {
  @Id @GeneratedValue private final Long id;

  @Relationship(type = "HAS_SUPERCLASS")
  private Class superclass;

  @Relationship(type = "BELONGS_TO")
  private Repository repository;

  @PersistenceConstructor
  public ClassRelation(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public Class getSuperclass() {
    return superclass;
  }

  public ClassRelation setSuperclass(Class superclass) {
    this.superclass = superclass;
    return this;
  }

  public Directory getRepository() {
    return repository;
  }

  public ClassRelation setRepository(Repository repository) {
    this.repository = repository;
    return this;
  }
}
