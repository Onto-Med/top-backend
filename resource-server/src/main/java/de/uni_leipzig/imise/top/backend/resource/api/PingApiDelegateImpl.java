package de.uni_leipzig.imise.top.backend.resource.api;

import de.uni_leipzig.imise.top.backend.api.PingApiDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PingApiDelegateImpl implements PingApiDelegate {
  @Override
  public ResponseEntity<Void> ping() {
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
