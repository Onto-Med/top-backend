package care.smith.top.backend.model;

import care.smith.top.model.*;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity(name = "expression")
public class ExpressionDao {
  @Id @GeneratedValue private Long id;

  private String functionId;

  private String entityId;
  private String constantId;
  private Boolean booleanValue;
  private LocalDateTime dateTimeValue;
  private BigDecimal numberValue;
  private String stringValue;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExpressionDao> arguments = null;

  public ExpressionDao() {}

  public ExpressionDao(@NotNull Expression expression) {
    functionId = expression.getFunctionId();
    entityId = expression.getEntityId();
    constantId = expression.getConstantId();
    value(expression.getValue());
    if (expression.getArguments() != null)
      arguments =
          expression.getArguments().stream().map(ExpressionDao::new).collect(Collectors.toList());
  }

  public ExpressionDao(
      String function,
      String entityId,
      String constantId,
      Value value,
      List<ExpressionDao> arguments) {
    this.functionId = function;
    this.entityId = entityId;
    this.constantId = constantId;
    value(value);
    this.arguments = arguments;
  }

  public Expression toApiModel() {
    Expression expression =
        new Expression().functionId(functionId).entityId(entityId).constantId(constantId);
    if (booleanValue != null) expression.value(new BooleanValue().value(booleanValue));
    if (dateTimeValue != null) expression.value(new DateTimeValue().value(dateTimeValue));
    if (numberValue != null) expression.value(new NumberValue().value(numberValue));
    if (stringValue != null) expression.value(new StringValue().value(stringValue));
    if (arguments != null)
      expression.setArguments(
          arguments.stream().map(ExpressionDao::toApiModel).collect(Collectors.toList()));
    return expression;
  }

  public Long getId() {
    return id;
  }

  public ExpressionDao id(Long id) {
    this.id = id;
    return this;
  }

  public String getFunctionId() {
    return functionId;
  }

  public ExpressionDao functionId(String functionId) {
    this.functionId = functionId;
    return this;
  }

  public String getEntityId() {
    return entityId;
  }

  public ExpressionDao entityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public String getConstantId() {
    return constantId;
  }

  public ExpressionDao constantId(String constantId) {
    this.constantId = constantId;
    return this;
  }

  public ExpressionDao value(Value value) {
    if (value != null) {
      if (value instanceof BooleanValue) booleanValue = ((BooleanValue) value).isValue();
      if (value instanceof DateTimeValue) dateTimeValue = ((DateTimeValue) value).getValue();
      if (value instanceof NumberValue) numberValue = ((NumberValue) value).getValue();
      if (value instanceof StringValue) stringValue = ((StringValue) value).getValue();
    }
    return this;
  }

  public List<ExpressionDao> getArguments() {
    return arguments;
  }

  public ExpressionDao arguments(List<ExpressionDao> arguments) {
    this.arguments = arguments;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ExpressionDao that = (ExpressionDao) o;
    return Objects.equals(getFunctionId(), that.getFunctionId())
        && Objects.equals(getEntityId(), that.getEntityId())
        && Objects.equals(getConstantId(), that.getConstantId())
        && Objects.equals(booleanValue, that.booleanValue)
        && Objects.equals(dateTimeValue, that.dateTimeValue)
        && Objects.equals(numberValue, that.numberValue)
        && Objects.equals(stringValue, that.stringValue)
        && Objects.equals(getArguments(), that.getArguments());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getFunctionId(),
        getEntityId(),
        getConstantId(),
        booleanValue,
        dateTimeValue,
        numberValue,
        stringValue,
        getArguments());
  }
}
