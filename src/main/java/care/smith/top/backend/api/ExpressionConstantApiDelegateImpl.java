package care.smith.top.backend.api;

import care.smith.top.model.Constant;
import care.smith.top.top_phenotypic_query.c2reasoner.C2R;
import care.smith.top.top_phenotypic_query.c2reasoner.constants.ConstantEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpressionConstantApiDelegateImpl implements ExpressionConstantApiDelegate {
  private final C2R calculator = new C2R();

  @Override
  public ResponseEntity<List<Constant>> getExpressionConstants() {
    return new ResponseEntity<>(
        calculator.getConstants().stream()
            .map(ConstantEntity::getConstant)
            .sorted(Comparator.comparing(Constant::getTitle))
            .collect(Collectors.toList()),
        HttpStatus.OK);
  }
}
