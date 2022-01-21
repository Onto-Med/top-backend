package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.resource.repository.ClassRepository;
import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntityService {
  @Autowired ClassRepository classRepository;

  public Entity createEntity(String organisationName, String repositoryName, Entity entity) {
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
    Class cls = classRepository.findClassByUuid(id);
    if (cls == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    Entity entity = new Entity();
    entity.setId(cls.getUuid());
    // ...
    return entity;
  }

  public void deleteEntity(
      String organisationName, String repositoryName, UUID id, Integer version, boolean permanent) {
    Class cls = classRepository.findClassByUuid(id);
    // TODO: delete annotations
    classRepository.delete(cls);
  }
}
