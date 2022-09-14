package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  public Annotatable addAnnotations(List<Annotation> annotations) {
    if (annotations == null) return this;
    AtomicInteger index = new AtomicInteger();
    return addAnnotations(
        annotations.stream()
            .map(a -> a.setIndex(index.incrementAndGet()))
            .collect(Collectors.toSet()));
  }

  public Set<Annotation> getAnnotations(String... property) {
    if (annotations == null) return new HashSet<>();
    List<String> propertyList = Arrays.asList(property);
    return annotations.stream()
        .filter(a -> propertyList.contains(a.getProperty()))
        .collect(Collectors.toSet());
  }

  public List<Annotation> getSortedAnnotations(String... property) {
    if (annotations == null) return new ArrayList<>();
    return getAnnotations(property).stream()
        .sorted(
            (a, b) -> {
              if (a.getIndex() == null) return b.getIndex() == null ? 0 : -1;
              if (b.getIndex() == null) return 1;
              return a.getIndex().compareTo(b.getIndex());
            })
        .collect(Collectors.toList());
  }

  public Optional<Annotation> getAnnotation(String... property) {
    return getSortedAnnotations(property).stream().findFirst();
  }

  public Set<Annotation> getAnnotations() {
    if (annotations == null) return new HashSet<>();
    return annotations;
  }

  public List<Annotation> getSortedAnnotations() {
    return getAnnotations().stream()
        .sorted(
            (a, b) -> {
              if (a.getIndex() == null) return b.getIndex() == null ? 0 : -1;
              if (b.getIndex() == null) return 1;
              return a.getIndex().compareTo(b.getIndex());
            })
        .collect(Collectors.toList());
  }

  public Annotatable setAnnotations(Set<Annotation> annotations) {
    this.annotations = annotations;
    return this;
  }
}
