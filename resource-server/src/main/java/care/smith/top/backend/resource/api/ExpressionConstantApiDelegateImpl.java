package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.ExpressionConstantApiDelegate;
import care.smith.top.backend.model.Constant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ExpressionConstantApiDelegateImpl implements ExpressionConstantApiDelegate {
  @Override
  public ResponseEntity<List<Constant>> getConstants() {
    return new ResponseEntity<>(
        Arrays.asList(
            new Constant().id("e"),
            new Constant().id("pi").title("π"),
            new Constant().id("true").title("True"),
            new Constant().id("false").title("False")),
        HttpStatus.OK);
  }
}
