package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
  @CreatedBy private String user;
  @CreatedDate private Instant createdAt;

  private int version;
  private String name;

  @Relationship(type = "HAS_EXPRESSION")
  private Set<Expression> expressions;

  @Relationship(type = "IS_EQUIVALENT_TO")
  private Set<ClassVersion> equivalentClasses;

  @Relationship(type = "IS_VERSION_OF")
  private Class aClass;

  @Relationship(type = "PREVIOUS_VERSION")
  private ClassVersion previousVersion;

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

  public Class getaClass() {
    return aClass;
  }

  public ClassVersion setaClass(Class aClass) {
    this.aClass = aClass;
    return this;
  }

  public Set<ClassVersion> getEquivalentClasses() {
    return equivalentClasses;
  }

  public ClassVersion setEquivalentClasses(Set<ClassVersion> equivalentClasses) {
    this.equivalentClasses = equivalentClasses;
    return this;
  }

  public Set<Expression> getExpressions() {
    return expressions;
  }

  public ClassVersion setExpressions(Set<Expression> expressions) {
    this.expressions = expressions;
    return this;
  }

  public int getVersion() {
    return version;
  }

  public ClassVersion setVersion(int version) {
    this.version = version;
    return this;
  }

  public Long getNodeVersion() {
    return nodeVersion;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getCreatedAtOffset() {
    if (getCreatedAt() == null) return null;
    return getCreatedAt().atOffset(ZoneOffset.UTC);
  }

  public String getUser() {
    return user;
  }

  public String getName() {
    return name;
  }

  public ClassVersion setName(String name) {
    this.name = name;
    return this;
  }

  public Long getId() {
    return id;
  }

  public ClassVersion getPreviousVersion() {
    return previousVersion;
  }

  public ClassVersion setPreviousVersion(ClassVersion previousVersion) {
    this.previousVersion = previousVersion;
    return this;
  }
}
