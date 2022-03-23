package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.ExpressionOperatorApiDelegate;
import care.smith.top.backend.model.ExpressionMultaryOperator;
import care.smith.top.backend.model.ExpressionOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class ExpressionOperatorApiDelegateImpl implements ExpressionOperatorApiDelegate {
  @Override
  public ResponseEntity<List<ExpressionOperator>> getExpressionOperators(String type) {
    switch (type) {
      case "math":
        return new ResponseEntity<>(getMathOperators(), HttpStatus.OK);
      case "boolean":
        return new ResponseEntity<>(getBooleanOperators(), HttpStatus.OK);
      default:
        throw new ResponseStatusException(
            HttpStatus.NOT_ACCEPTABLE, "Expression operator type is not supported.");
    }
  }

  private List<ExpressionOperator> getBooleanOperators() {
    return Arrays.asList(
        new ExpressionMultaryOperator()
            .required(2)
            .id("intersection")
            .title("and")
            .type(ExpressionOperator.TypeEnum.MULTARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX),
        new ExpressionMultaryOperator()
            .required(2)
            .id("union")
            .title("or")
            .type(ExpressionOperator.TypeEnum.MULTARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX),
        new ExpressionOperator()
            .id("complement")
            .title("and")
            .type(ExpressionOperator.TypeEnum.UNARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX),
        new ExpressionOperator()
            .id("entity")
            .title("entity")
            .type(ExpressionOperator.TypeEnum.UNARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX));
  }

  private List<ExpressionOperator> getMathOperators() {
    return Arrays.asList(
        new ExpressionOperator()
            .id("addition")
            .title("+")
            .type(ExpressionOperator.TypeEnum.BINARY)
            .representation(ExpressionOperator.RepresentationEnum.INFIX),
        new ExpressionOperator()
            .id("subtraction")
            .title("-")
            .type(ExpressionOperator.TypeEnum.BINARY)
            .representation(ExpressionOperator.RepresentationEnum.INFIX),
        new ExpressionOperator()
            .id("multiplication")
            .title("*")
            .type(ExpressionOperator.TypeEnum.BINARY)
            .representation(ExpressionOperator.RepresentationEnum.INFIX),
        new ExpressionOperator()
            .id("division")
            .title("/")
            .type(ExpressionOperator.TypeEnum.BINARY)
            .representation(ExpressionOperator.RepresentationEnum.INFIX),
        new ExpressionOperator()
            .id("exponentiation")
            .title("^")
            .type(ExpressionOperator.TypeEnum.BINARY)
            .representation(ExpressionOperator.RepresentationEnum.INFIX),
        new ExpressionMultaryOperator()
            .required(2)
            .id("minimum")
            .title("min")
            .type(ExpressionOperator.TypeEnum.MULTARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX),
        new ExpressionMultaryOperator()
            .required(2)
            .id("maximum")
            .title("max")
            .type(ExpressionOperator.TypeEnum.MULTARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX),
        new ExpressionOperator()
            .id("entity")
            .title("entity")
            .type(ExpressionOperator.TypeEnum.UNARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX),
        new ExpressionOperator()
            .id("constant")
            .title("constant")
            .type(ExpressionOperator.TypeEnum.UNARY)
            .representation(ExpressionOperator.RepresentationEnum.PREFIX));
  }
}
