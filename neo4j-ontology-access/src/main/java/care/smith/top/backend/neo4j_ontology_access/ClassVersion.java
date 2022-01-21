package care.smith.top.backend.neo4j_ontology_access;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.UUID;

/**
 * Example:
 *
 * <p>(:Class { id: '3fa85f64-5717-4562-b3fc-2c963f66afa6' }) -[:HAS_ANNOTATION]-> (:Annotation)
 */
@Node
public class ClassVersion extends Annotatable {
  @Id private UUID id;
  private int version;

  public ClassVersion() {
    this.id = UUID.randomUUID();
  }

  public ClassVersion(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
