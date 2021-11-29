package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.OrganisationNameApiDelegate;
import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.Organisation;
import care.smith.top.data.tables.records.DirectoryRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static care.smith.top.data.Tables.*;

@Service
public class OrganisationNameApiDelegateImpl implements OrganisationNameApiDelegate {
  @Autowired
  DSLContext context;
  @Override
  public ResponseEntity<Entity> createEntity(String organisationName, String repositoryName, Entity entity, List<String> include) {
    return new ResponseEntity<>(entity, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Organisation> deleteOrganisationByName(String organisationName, List<String> include) {
    DirectoryRecord record = context.fetchOne(DIRECTORY, DIRECTORY.NAME.eq(organisationName));
    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    Organisation response = new Organisation();
    response.setOrganisationId(record.getDirectoryId());
    response.setName(record.getName());
    response.setDescription(record.getDescription());
    response.setCreatedAt(record.getCreatedAt());

    record.delete();

    return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
  }
}