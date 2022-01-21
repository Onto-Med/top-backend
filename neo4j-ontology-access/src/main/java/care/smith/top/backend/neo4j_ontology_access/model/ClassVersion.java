package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
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
  @Id @GeneratedValue private long id;
  @Version private long version;

  @Relationship(type = "HAS_EXPRESSION")
  private Set<Expression> expressions;

  @Relationship(type = "IS_EQUIVALENT_TO")
  private Set<ClassVersion> equivalentClasses;

  public ClassVersion() {}

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

  public ClassVersion addEquivalentClass(ClassVersion equivalentClass) {
    return addEquivalentClasses(Collections.singleton(equivalentClass));
  }

  public ClassVersion addEquivalentClasses(Set<ClassVersion> equivalentClasses) {
    if (this.equivalentClasses == null) this.equivalentClasses = new HashSet<>();
    this.equivalentClasses.addAll(equivalentClasses);
    return this;
  }

  public ClassVersion setEquivalentClasses(Set<ClassVersion> equivalentClasses) {
    this.equivalentClasses = equivalentClasses;
    return this;
  }

  public long getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }
}
