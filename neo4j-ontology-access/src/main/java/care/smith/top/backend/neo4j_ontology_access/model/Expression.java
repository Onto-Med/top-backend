package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Example:
 *
 * <p>(:ClassVersion) <-[:HAS_COMPONENT]- (:Expression { expression: 'has_part some A', type:
 * 'class_expression', index: 1 }) <-[:HAS_EXPRESSION]- (:ClassVersion)
 */
@Node
public class Expression {
  @Id @GeneratedValue private Long id;

  private String type;
  private Integer index;
  private Set<ClassVersion> components;

  public Expression() {}

  public Expression addComponent(ClassVersion component) {
    return addComponents(Collections.singleton(component));
  }

  public Expression addComponents(Set<ClassVersion> components) {
    if (this.components == null) this.components = new HashSet<>();
    this.components.addAll(components);
    return this;
  }

  public Long getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Expression setType(String type) {
    this.type = type;
    return this;
  }

  public Integer getIndex() {
    return index;
  }

  public Expression setIndex(Integer index) {
    this.index = index;
    return this;
  }

  public Set<ClassVersion> getComponents() {
    return components;
  }

  public Expression setComponents(Set<ClassVersion> components) {
    this.components = components;
    return this;
  }
}