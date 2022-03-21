package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.DefaultApiDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PingApiDelegateImpl implements DefaultApiDelegate {
  @Override
  public ResponseEntity<Void> ping() {
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
