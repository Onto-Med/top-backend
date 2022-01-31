package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.Node;

/** Repositories are included in {@link Directory}s and can contain ontologies and classes. */
@Node
public class Repository extends Directory {
  /** Determins whether this repository is a primary (aka. public) repository. */
  private boolean primary;

  public Repository() {
    super();
  }

  @PersistenceConstructor
  public Repository(String id) {
    super(id);
  }

  public boolean isPrimary() {
    return primary;
  }

  public Repository setPrimary(boolean primary) {
    this.primary = primary;
    return this;
  }
}
