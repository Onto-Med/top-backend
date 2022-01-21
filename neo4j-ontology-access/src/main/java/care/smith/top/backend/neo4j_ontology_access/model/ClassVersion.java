package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Example:
 *
 * <p>(:ClassVersion { id: '3fa85f64-5717-4562-b3fc-2c963f66afa6' }) -[:HAS_ANNOTATION]->
 * (:Annotation)
 */
@Node
public class ClassVersion extends Annotatable {
  @Id private UUID id;
  @Version private long version;

  @Relationship(type = "HAS_EXPRESSION")
  private Set<Expression> expressions;

  public ClassVersion() {
    this.id = UUID.randomUUID();
  }

  public ClassVersion(UUID id) {
    this.id = id;
  }

  public ClassVersion addExpression(Expression expression) {
    return addExpressions(Collections.singleton(expression));
  }

  public ClassVersion addExpressions(Set<Expression> expressions) {
    if (this.expressions == null) this.expressions = new HashSet<>();
    this.expressions.addAll(expressions);
    return this;
  }

  public void setExpressions(Set<Expression> expressions) {
    this.expressions = expressions;
  }

  public UUID getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }
}
