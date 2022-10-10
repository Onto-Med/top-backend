package care.smith.top.backend.api;

import care.smith.top.model.ExpressionFunction;
import care.smith.top.backend.util.OntoModelMapper;
import care.smith.top.simple_onto_api.calculator.Calculator;
import care.smith.top.simple_onto_api.calculator.functions.aggregate.*;
import care.smith.top.simple_onto_api.calculator.functions.arithmetic.*;
import care.smith.top.simple_onto_api.calculator.functions.bool.And;
import care.smith.top.simple_onto_api.calculator.functions.bool.Not;
import care.smith.top.simple_onto_api.calculator.functions.bool.Or;
import care.smith.top.simple_onto_api.calculator.functions.comparison.*;
import care.smith.top.simple_onto_api.calculator.functions.date_time.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExpressionFunctionApiDelegateImpl implements ExpressionFunctionApiDelegate {
  public static final List<String> EXCLUDED_FUNCTION_IDS = Arrays.asList("switch", "list");
  private Calculator calculator = new Calculator();

  @Override
  public ResponseEntity<List<ExpressionFunction>> getExpressionFunctions(String type) {
    if ("math".equals(type)) return new ResponseEntity<>(getMathFunctions(), HttpStatus.OK);
    throw new ResponseStatusException(
        HttpStatus.NOT_ACCEPTABLE, "Expression operator type is not supported.");
  }

  private List<ExpressionFunction> getBooleanFunctions() {
    return Stream.of(And.get(), Or.get(), Not.get())
        .map(OntoModelMapper::map)
        .collect(Collectors.toList());
  }

  private List<ExpressionFunction> getMathFunctions() {
    return calculator.getFunctions().stream()
        .filter(f -> !EXCLUDED_FUNCTION_IDS.contains(f.getId()))
        .map(OntoModelMapper::map)
        .sorted(Comparator.comparing(ExpressionFunction::getTitle))
        .collect(Collectors.toList());
  }
}
