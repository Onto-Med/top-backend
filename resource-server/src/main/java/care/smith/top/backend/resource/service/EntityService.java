package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.Phenotype;
import care.smith.top.data.tables.records.ClassRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static care.smith.top.data.Tables.CLASS;

@Service
public class EntityService {
  @Autowired DSLContext context;

  public Entity loadEntityById(String id) {
    ClassRecord record = context.fetchOne(CLASS, CLASS.UUID.eq(UUID.fromString(id)));
    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    return mapToEntity(record);
  }

  private Entity mapToEntity(ClassRecord record) {
    // TODO: mapping from DB
    return new Entity();
  }
}
