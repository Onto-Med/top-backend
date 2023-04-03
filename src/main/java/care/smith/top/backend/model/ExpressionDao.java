package care.smith.top.backend.model;

import care.smith.top.model.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity(name = "expression")
public class ExpressionDao {
  @Id @GeneratedValue private Long id;

  private String functionId;
  private String entityId;
  private String constantId;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExpressionDao> arguments = null;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ValueDao> values = null;

  public ExpressionDao() {}

  public ExpressionDao(@NotNull Expression expression) {
    functionId = expression.getFunctionId();
    entityId = expression.getEntityId();
    constantId = expression.getConstantId();
    if (expression.getValues() != null)
      values = expression.getValues().stream().map(ValueDao::new).collect(Collectors.toList());
    if (expression.getArguments() != null)
      arguments =
          expression.getArguments().stream()
              .map(a -> a == null ? null : new ExpressionDao(a))
              .collect(Collectors.toList());
  }

  public ExpressionDao(
      String function,
      String entityId,
      String constantId,
      List<ValueDao> values,
      List<ExpressionDao> arguments) {
    this.functionId = function;
    this.entityId = entityId;
    this.constantId = constantId;
    this.values = values;
    this.arguments = arguments;
  }

  public Expression toApiModel() {
    Expression expression =
        new Expression().functionId(functionId).entityId(entityId).constantId(constantId);
    if (arguments != null)
      expression.setArguments(
          arguments.stream()
              .map(a -> a == null ? null : a.toApiModel())
              .collect(Collectors.toList()));
    if (values != null)
      expression.setValues(values.stream().map(ValueDao::toApiModel).collect(Collectors.toList()));
    return expression;
  }

  public Long getId() {
    return id;
  }

  public ExpressionDao id(@NotNull Long id) {
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

  public List<ExpressionDao> getArguments() {
    return arguments;
  }

  public ExpressionDao arguments(List<ExpressionDao> arguments) {
    this.arguments = arguments;
    return this;
  }

  public List<ValueDao> getValues() {
    return values;
  }

  public ExpressionDao setValues(List<ValueDao> values) {
    this.values = values;
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
        && Objects.equals(getValues(), that.getValues())
        && Objects.equals(getArguments(), that.getArguments());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getFunctionId(), getEntityId(), getConstantId(), getValues(), getArguments());
  }
}
