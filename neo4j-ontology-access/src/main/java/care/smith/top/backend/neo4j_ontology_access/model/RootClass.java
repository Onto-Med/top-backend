package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class RootClass {
  @RelationshipId private Long id;

  @TargetNode private Class rootClass;

  private Integer index;

  public RootClass(Class rootClass, Integer index) {
    this.rootClass = rootClass;
    this.index = index;
  }
}
