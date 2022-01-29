package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Repositories are included in {@link Directory}s and can contain ontologies and classes. */
@Node({ "Repository", "RelationOwner" })
public class Repository extends Directory implements ClassRelationOwner {
  /** Determins whether this repository is a primary (aka. public) repository. */
  private boolean primary;

  @Relationship(type = "HAS_ROOT_CLASS")
  private Set<RootClass> rootClasses = null;

  public Repository() {
    super();
    this.setTypes(Collections.singleton(this.getClass().getName()));
  }

  @PersistenceConstructor
  public Repository(String id) {
    super(id);
    this.setTypes(Collections.singleton(this.getClass().getName()));
  }

  @Override
  public ClassRelationOwner addRootClass(RootClass rootClass) {
    if (rootClasses == null) rootClasses = new HashSet<>();
    rootClasses.add(rootClass);
    return this;
  }

  @Override
  public Set<RootClass> getRootClasses() {
    return rootClasses;
  }

  @Override
  public ClassRelationOwner setRootClasses(Set<RootClass> rootClasses) {
    this.rootClasses = rootClasses;
    return this;
  }

  public boolean isPrimary() {
    return primary;
  }

  public Repository setPrimary(boolean primary) {
    this.primary = primary;
    return this;
  }
}
