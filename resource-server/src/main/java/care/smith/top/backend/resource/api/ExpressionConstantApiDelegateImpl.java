package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.ExpressionConstantApiDelegate;
import care.smith.top.backend.model.Constant;
import care.smith.top.backend.resource.util.OntoModelMapper;
import care.smith.top.simple_onto_api.calculator.Calculator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpressionConstantApiDelegateImpl implements ExpressionConstantApiDelegate {
  private Calculator calculator = new Calculator();

  @Override
  public ResponseEntity<List<Constant>> getExpressionConstants() {
    return new ResponseEntity<>(
        calculator.getConstants().stream()
            .map(OntoModelMapper::map)
            .sorted(Comparator.comparing(Constant::getTitle))
            .collect(Collectors.toList()),
        HttpStatus.OK);
  }
}
