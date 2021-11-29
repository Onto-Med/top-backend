package de.uni_leipzig.imise.top.backend.resource.service;

import de.uni_leipzig.imise.top.backend.model.Phenotype;
import de.uni_leipzig.imise.top.data.tables.records.ClassRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static de.uni_leipzig.imise.top.data.Tables.CLASS;

@Service
public class PhenotypeService {
  @Autowired DSLContext context;

  public Phenotype loadPhenotypeById(String id) {
    ClassRecord record = context.fetchOne(CLASS, CLASS.UUID.eq(UUID.fromString(id)));

    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    Phenotype phenotype = new Phenotype();
    // TODO: mapping from DB
    return phenotype;
  }

  private Phenotype maptoPhenotype(ClassRecord record) {
    return null;
  }
}
