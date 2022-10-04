package care.smith.top.backend.model;

import care.smith.top.model.Expression;
import care.smith.top.model.Value;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "expression")
public class ExpressionDao {
  @Id @GeneratedValue private Long id;

  @Column(nullable = false)
  private String functionId;

  private String entityId;
  private String constantId;
  private Value value;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExpressionDao> arguments = null;

  public ExpressionDao() {}

  public ExpressionDao(@NotNull Expression expression) {
    functionId = expression.getFunctionId();
    entityId = expression.getEntityId();
    constantId = expression.getConstantId();
    value = expression.getValue();
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
    this.value = value;
    this.arguments = arguments;
  }

  public Expression toApiModel() {
    Expression expression =
        new Expression()
            .functionId(functionId)
            .entityId(entityId)
            .constantId(constantId)
            .value(value);
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

  public Value getValue() {
    return value;
  }

  public ExpressionDao value(Value value) {
    this.value = value;
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

    if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
    if (!getFunctionId().equals(that.getFunctionId())) return false;
    if (!getEntityId().equals(that.getEntityId())) return false;
    if (!getConstantId().equals(that.getConstantId())) return false;
    if (!getValue().equals(that.getValue())) return false;
    return getArguments().equals(that.getArguments());
  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + getFunctionId().hashCode();
    result = 31 * result + getEntityId().hashCode();
    result = 31 * result + getConstantId().hashCode();
    result = 31 * result + getValue().hashCode();
    result = 31 * result + getArguments().hashCode();
    return result;
  }
}
