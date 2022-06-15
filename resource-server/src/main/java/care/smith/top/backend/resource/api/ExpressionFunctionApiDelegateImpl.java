package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.ExpressionFunctionApiDelegate;
import care.smith.top.backend.model.ExpressionFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class ExpressionFunctionApiDelegateImpl implements ExpressionFunctionApiDelegate {
  @Override
  public ResponseEntity<List<ExpressionFunction>> getExpressionFunctions(String type) {
    if (type != null) {
      switch (type) {
        case "math":
          return new ResponseEntity<>(getMathFunctions(), HttpStatus.OK);
        case "boolean":
          return new ResponseEntity<>(getBooleanFunctions(), HttpStatus.OK);
      }
    }
    throw new ResponseStatusException(
        HttpStatus.NOT_ACCEPTABLE, "Expression operator type is not supported.");
  }

  private List<ExpressionFunction> getBooleanFunctions() {
    return Arrays.asList(
        new ExpressionFunction()
            .id("intersection")
            .title("and")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(2),
        new ExpressionFunction()
            .id("union")
            .title("or")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(2),
        new ExpressionFunction()
            .id("complement")
            .title("not")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(1)
            .maxArgumentNumber(1),
        new ExpressionFunction()
            .id("entity")
            .title("entity")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(1)
            .maxArgumentNumber(1));
  }

  private List<ExpressionFunction> getMathFunctions() {
    return Arrays.asList(
        new ExpressionFunction()
            .id("addition")
            .title("+")
            .notation(ExpressionFunction.NotationEnum.INFIX)
            .minArgumentNumber(2)
            .maxArgumentNumber(2),
        new ExpressionFunction()
            .id("subtraction")
            .title("-")
            .notation(ExpressionFunction.NotationEnum.INFIX)
            .minArgumentNumber(2)
            .maxArgumentNumber(2),
        new ExpressionFunction()
            .id("multiplication")
            .title("*")
            .notation(ExpressionFunction.NotationEnum.INFIX)
            .minArgumentNumber(2)
            .maxArgumentNumber(2),
        new ExpressionFunction()
            .id("division")
            .title("/")
            .notation(ExpressionFunction.NotationEnum.INFIX)
            .minArgumentNumber(2)
            .maxArgumentNumber(2),
        new ExpressionFunction()
            .id("exponentiation")
            .title("^")
            .notation(ExpressionFunction.NotationEnum.INFIX)
            .minArgumentNumber(2)
            .maxArgumentNumber(2),
        new ExpressionFunction()
            .id("minimum")
            .title("min")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(2),
        new ExpressionFunction()
            .id("maximum")
            .title("max")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(2),
        new ExpressionFunction()
            .id("entity")
            .title("entity")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(1)
            .maxArgumentNumber(1),
        new ExpressionFunction()
            .id("constant")
            .title("constant")
            .notation(ExpressionFunction.NotationEnum.PREFIX)
            .minArgumentNumber(1)
            .maxArgumentNumber(1));
  }
}
