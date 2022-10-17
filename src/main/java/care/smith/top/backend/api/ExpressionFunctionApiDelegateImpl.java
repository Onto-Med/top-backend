package care.smith.top.backend.api;

import care.smith.top.model.ExpressionFunction;
import care.smith.top.top_phenotypic_query.c2reasoner.C2R;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.FunctionEntity;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.bool.And;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.bool.Not;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.bool.Or;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExpressionFunctionApiDelegateImpl implements ExpressionFunctionApiDelegate {
  public static final List<String> EXCLUDED_FUNCTION_IDS = Arrays.asList("switch", "list", "restrict");
  private final C2R calculator = new C2R();

  @Override
  public ResponseEntity<List<ExpressionFunction>> getExpressionFunctions(String type) {
    if ("math".equals(type)) return new ResponseEntity<>(getMathFunctions(), HttpStatus.OK);
    throw new ResponseStatusException(
        HttpStatus.NOT_ACCEPTABLE, "Expression operator type is not supported.");
  }

  private List<ExpressionFunction> getBooleanFunctions() {
    return Stream.of(And.get(), Or.get(), Not.get())
        .map(FunctionEntity::getFunction)
        .collect(Collectors.toList());
  }

  private List<ExpressionFunction> getMathFunctions() {
    return calculator.getFunctions().stream()
        .map(FunctionEntity::getFunction)
        .filter(f -> !EXCLUDED_FUNCTION_IDS.contains(f.getId()))
        .sorted(Comparator.comparing(ExpressionFunction::getTitle))
        .collect(Collectors.toList());
  }
}
