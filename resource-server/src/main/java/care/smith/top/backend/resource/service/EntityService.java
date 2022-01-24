package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.neo4j_ontology_access.model.Annotatable;
import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import care.smith.top.backend.resource.repository.AnnotationRepository;
import care.smith.top.backend.resource.repository.ClassRepository;
import care.smith.top.backend.resource.repository.ClassVersionRepository;
import care.smith.top.backend.resource.repository.ExpressionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntityService {
  @Autowired ClassRepository classRepository;
  @Autowired ClassVersionRepository classVersionRepository;
  @Autowired AnnotationRepository annotationRepository;
  @Autowired ExpressionRepository expressionRepository;

  @Transactional
  public Entity createEntity(String organisationName, String repositoryName, Entity entity) {
    if (classRepository.existsById(entity.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    Class cls = new Class(entity.getId());
    ClassVersion version = new ClassVersion();

    version
        .addAnnotations(
            entity.getTitles().stream()
                .map(t -> new Annotation("title", t.getText(), t.getLang(), null))
                .collect(Collectors.toSet()))
        .addAnnotations(
            entity.getSynonyms().stream()
                .map(s -> new Annotation("synonym", s.getText(), s.getLang(), null))
                .collect(Collectors.toSet()))
        .addAnnotations(
            entity.getDescriptions().stream()
                .map(d -> new Annotation("description", d.getText(), d.getLang(), null))
                .collect(Collectors.toSet()));

    cls.createVersion(version).setCurrentVersion(version);

    classRepository.save(cls);
    return entity;
  }

  public Entity loadEntity(
      String organisationName, String repositoryName, UUID id, Integer version) {
    Optional<Class> cls = classRepository.findById(id);
    if (cls.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    Entity entity = new Entity();
    entity.setId(cls.get().getUuid());
    // ...
    return entity;
  }

  @Transactional
  public void deleteEntity(
      String organisationName, String repositoryName, UUID id, Integer version, boolean permanent) {
    Optional<Class> cls = classRepository.findById(id);
    if (cls.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    ClassVersion classVersion;
    if (version == null) {
      classVersion = cls.get().getCurrentVersion();
    } else {
      Optional<ClassVersion> optional =
          cls.get().getVersions().stream().filter(v -> v.getVersion() == version).findFirst();
      if (optional.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      classVersion = optional.get();
    }

    if (permanent) {
      deleteAnnotations(classVersion);
      expressionRepository.deleteAll(classVersion.getExpressions());
      classVersionRepository.delete(classVersion);
    } else {
      if (!classVersion.isHidden()) {
        classVersion.setHiddenAt(LocalDateTime.now());
        classVersionRepository.save(classVersion);
      }
    }
  }

  /**
   * Recursively delete all annotations of an annotatable object and its annotations.
   * @param annotatable Annotatable object of which annotations will be deleted.
   */
  private void deleteAnnotations(Annotatable annotatable) {
    annotatable.getAnnotations().forEach(this::deleteAnnotations);
    annotationRepository.deleteAll(annotatable.getAnnotations());
  }
}
