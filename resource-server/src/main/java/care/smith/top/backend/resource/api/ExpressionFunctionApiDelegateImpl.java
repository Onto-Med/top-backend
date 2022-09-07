package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.ExpressionFunctionApiDelegate;
import care.smith.top.backend.model.ExpressionFunction;
import care.smith.top.backend.resource.util.OntoModelMapper;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExpressionFunctionApiDelegateImpl implements ExpressionFunctionApiDelegate {
  @Override
  public ResponseEntity<List<ExpressionFunction>> getExpressionFunctions(String type) {
    if ("math".equals(type)) return new ResponseEntity<>(getMathFunctions(), HttpStatus.OK);
    throw new ResponseStatusException(
        HttpStatus.NOT_ACCEPTABLE, "Expression operator type is not supported.");
  }

  private List<ExpressionFunction> getBooleanFunctions() {
    List<ExpressionFunction> list =
        new java.util.ArrayList<>(
            Collections.singletonList(
                new ExpressionFunction()
                    .id("entity")
                    .title("entity")
                    .notation(ExpressionFunction.NotationEnum.PREFIX)
                    .minArgumentNumber(1)
                    .maxArgumentNumber(1)));
    list.addAll(
        Stream.of(And.get(), Or.get(), Not.get())
            .map(OntoModelMapper::map)
            .collect(Collectors.toList()));
    return list;
  }

  private List<ExpressionFunction> getMathFunctions() {
    List<ExpressionFunction> list =
        new java.util.ArrayList<>(
            Arrays.asList(
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
                    .maxArgumentNumber(1)));
    list.addAll(
        Stream.of(
                Add.get(),
                Divide.get(),
                Multiply.get(),
                Power.get(),
                Subtract.get(),
                Avg.get(),
                Count.get(),
                First.get(),
                Last.get(),
                Max.get(),
                Min.get(),
                Eq.get(),
                Ge.get(),
                Gt.get(),
                Le.get(),
                Lt.get(),
                Ne.get(),
                Date.get(),
                DiffDays.get(),
                DiffMonths.get(),
                DiffYears.get(),
                PlusDays.get(),
                PlusMonths.get(),
                PlusYears.get(),
                And.get(),
                Or.get(),
                Not.get())
            .map(OntoModelMapper::map)
            .sorted(Comparator.comparing(ExpressionFunction::getTitle))
            .collect(Collectors.toList()));
    return list;
  }
}
