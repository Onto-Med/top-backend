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
  @Id @GeneratedValue private Long id;

  /** Use this property to sort subclasses below a superclass. */
  private Integer index;

  @Relationship(type = "HAS_SUPERCLASS")
  private Class superclass;

  private String ownerId;

  @PersistenceConstructor
  public ClassRelation() {}

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

  public String getOwnerId() {
    return ownerId;
  }

  public ClassRelation setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public Integer getIndex() {
    return index;
  }

  public ClassRelation setIndex(Integer index) {
    this.index = index;
    return this;
  }
}
