package de.uni_leipzig.imise.top.backend.api;

import de.uni_leipzig.imise.top.backend.model.Entity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganisationNameApiDelegateImpl implements OrganisationNameApiDelegate {
  @Override
  public ResponseEntity<Entity> createEntity(String organisationName, String repositoryName, Entity entity, List<String> include) {
    return new ResponseEntity<>(entity, HttpStatus.CREATED);
  }
}
