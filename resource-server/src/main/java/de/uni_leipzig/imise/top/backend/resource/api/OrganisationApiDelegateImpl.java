package de.uni_leipzig.imise.top.backend.resource.api;

import de.uni_leipzig.imise.top.backend.api.OrganisationApiDelegate;
import de.uni_leipzig.imise.top.backend.model.Organisation;
import de.uni_leipzig.imise.top.data.tables.records.DirectoryRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.uni_leipzig.imise.top.data.Tables.*;

@Service
public class OrganisationApiDelegateImpl implements OrganisationApiDelegate {
  @Autowired DSLContext context;

  @Override
  public ResponseEntity<Organisation> createOrganisation(Organisation organisation, List<String> include) {
    DirectoryRecord newRecord =
      context.newRecord(DIRECTORY)
        .setName(organisation.getName())
        .setDescription(organisation.getDescription())
        .setType("organisation");
    newRecord.store();
    newRecord.refresh();

    Organisation created = new Organisation();
    created.setOrganisationId(newRecord.getDirectoryId());
    created.setName(newRecord.getName());
    created.setDescription(newRecord.getDescription());
    created.setCreatedAt(newRecord.getCreatedAt());

    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }
}
