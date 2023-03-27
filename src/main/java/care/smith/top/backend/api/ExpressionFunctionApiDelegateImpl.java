package care.smith.top.backend.api;

import care.smith.top.model.ExpressionFunction;
import care.smith.top.top_phenotypic_query.c2reasoner.C2R;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.FunctionEntity;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.advanced.Switch;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpressionFunctionApiDelegateImpl implements ExpressionFunctionApiDelegate {
  public static final List<String> EXCLUDED_FUNCTION_IDS =
      Collections.singletonList(Switch.get().getFunctionId());
  private final C2R calculator = new C2R();

  @Override
  public ResponseEntity<List<ExpressionFunction>> getExpressionFunctions(String type) {
    return new ResponseEntity<>(
        calculator.getFunctions().stream()
            .filter(f -> StringUtils.isBlank(type) || type.equals(f.getType()))
            .map(FunctionEntity::getFunction)
            .filter(f -> !EXCLUDED_FUNCTION_IDS.contains(f.getId()))
            .sorted(Comparator.comparing(ExpressionFunction::getTitle))
            .collect(Collectors.toList()),
        HttpStatus.OK);
  }
}
