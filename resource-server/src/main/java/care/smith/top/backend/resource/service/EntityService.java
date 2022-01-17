package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.neo4j_ontology_access.Class;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class EntityService {
  @Autowired OntologyRepository ontologyRepository;

  public Entity createEntity(String organisationName, String repositoryName, Entity entity) {
    Class cls = new Class(entity.getId().toString());
    ontologyRepository.save(cls);
    return entity;
  }

  public Entity loadEntity(
      String organisationName, String repositoryName, UUID id, Integer version) {
    Class cls = ontologyRepository.findClassByName(id.toString());
    if (cls == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    Entity entity = new Entity();
    entity.setId(UUID.fromString(cls.getName()));
    return entity;
  }

  public void deleteEntity(
      String organisationName, String repositoryName, UUID id, Integer version, boolean permanent) {
    throw new NotImplementedException("");
  }
}
