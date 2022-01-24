package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Example:
 *
 * <p>(:ClassVersion { id: '3fa85f64-5717-4562-b3fc-2c963f66afa6', name: 'weight', createdAt:
 * datetime(), hiddenAt: null }) -[:HAS_ANNOTATION]-> (:Annotation)
 */
@Node
public class ClassVersion extends Annotatable {
  @Id @GeneratedValue private Long id;
  @Version private Long nodeVersion;
  @CreatedBy private User user;
  @CreatedDate private LocalDateTime createdAt;
  private Long version;
  private LocalDateTime hiddenAt;
  private String name;

  @Relationship(type = "HAS_EXPRESSION")
  private Set<Expression> expressions;

  @Relationship(type = "IS_EQUIVALENT_TO")
  private Set<ClassVersion> equivalentClasses;

  public ClassVersion() {}

  @PersistenceConstructor
  public ClassVersion(Long id) {
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

  public boolean isHidden() {
    return hiddenAt != null;
  }

  public Set<Expression> getExpressions() {
    return expressions;
  }

  public ClassVersion setExpressions(Set<Expression> expressions) {
    this.expressions = expressions;
    return this;
  }

  public long getVersion() {
    return version;
  }

  public ClassVersion setVersion(Long version) {
    this.version = version;
    return this;
  }

  public Long getNodeVersion() {
    return nodeVersion;
  }

  public LocalDateTime getHiddenAt() {
    return hiddenAt;
  }

  public ClassVersion setHiddenAt(LocalDateTime hiddenAt) {
    this.hiddenAt = hiddenAt;
    return this;
  }
}
