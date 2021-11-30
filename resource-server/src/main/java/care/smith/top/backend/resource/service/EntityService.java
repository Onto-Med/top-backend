package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Entity;
import care.smith.top.data.tables.records.ClassVersionRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static care.smith.top.data.Tables.CLASS_VERSION;

@Service
public class EntityService {
  @Autowired DSLContext context;

  public Entity loadEntity(
      String organisationName, String repositoryName, UUID id, Integer version) {
    ClassVersionRecord record = loadEntityRecord(organisationName, repositoryName, id, version);
    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    return mapToEntity(record);
  }

  public void deleteEntity(
      String organisationName, String repositoryName, UUID id, Integer version) {
    ClassVersionRecord record = loadEntityRecord(organisationName, repositoryName, id, version);
    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    record.delete();
  }

  private Entity mapToEntity(ClassVersionRecord record) {
    Entity entity = new Entity();
    // TODO: implement mapping from DB...

    return entity;
  }

  private ClassVersionRecord loadEntityRecord(
      String organisationName, String repositoryName, UUID id, Integer version) {
    // TODO: filter by organisationName and repositoryName
    return context.fetchOne(
        CLASS_VERSION, CLASS_VERSION.class_().UUID.eq(id).and(CLASS_VERSION.VERSION.eq(version)));
  }
}
