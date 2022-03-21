package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * This class describes relations between classes (super-sub-class relations). Relations always
 * belong to a repository or ontology. If there is no superclass, the subclass is a root node.
 */
@RelationshipProperties
public class ClassRelation implements Cloneable {
  @RelationshipId @GeneratedValue private Long id;
  @TargetNode private Class superclass;

  /** Use this property to sort subclasses below a superclass. */
  private Integer index;

  /** This is the ID of the repository or ontology, where this relation was defined. */
  private String ownerId;

  @PersistenceConstructor
  public ClassRelation() {}

  public ClassRelation(Class superClass, String ownerId, Integer index) {
    this.superclass = superClass;
    this.ownerId = ownerId;
    this.index = index;
  }

  @Override
  public ClassRelation clone() {
    return new ClassRelation().setOwnerId(ownerId).setIndex(index).setSuperclass(superclass);
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
