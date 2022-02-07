package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Annotatable {
  @Relationship(type = "HAS_ANNOTATION")
  protected Set<Annotation> annotations;

  public Annotatable addAnnotation(Annotation annotation) {
    return addAnnotations(Collections.singleton(annotation));
  }

  public Annotatable addAnnotations(Set<Annotation> annotations) {
    if (annotations == null) return this;
    if (this.annotations == null) this.annotations = new HashSet<>();
    this.annotations.addAll(
        annotations.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    return this;
  }

  public Set<Annotation> getAnnotations(String property) {
    if (annotations == null) return new HashSet<>();
    return annotations.stream()
        .filter(a -> property.equals(a.getProperty()))
        .collect(Collectors.toSet());
  }

  public Optional<Annotation> getAnnotation(String property) {
    return getAnnotations(property).stream().findFirst();
  }

  public Set<Annotation> getAnnotations() {
    return annotations;
  }

  public Annotatable setAnnotations(Set<Annotation> annotations) {
    this.annotations = annotations;
    return this;
  }
}
