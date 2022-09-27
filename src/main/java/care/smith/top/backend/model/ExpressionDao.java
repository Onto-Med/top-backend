package care.smith.top.backend.model;

import care.smith.top.model.Expression;
import care.smith.top.model.ExpressionValue;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "expression")
public class ExpressionDao {
  @Id @GeneratedValue private Long id;

  private String function;

  private String entityId;
  private ExpressionValue value;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExpressionDao> arguments = null;

  public ExpressionDao() {}

  public ExpressionDao(Expression expression) {
    function = expression.getFunction();
    entityId = expression.getEntityId();
    value = expression.getValue();
    arguments =
        expression.getArguments().stream().map(ExpressionDao::new).collect(Collectors.toList());
  }

  public ExpressionDao(
      String function, String entityId, ExpressionValue value, List<ExpressionDao> arguments) {
    this.function = function;
    this.entityId = entityId;
    this.value = value;
    this.arguments = arguments;
  }

  public Long getId() {
    return id;
  }

  public ExpressionDao id(Long id) {
    this.id = id;
    return this;
  }

  public String getFunction() {
    return function;
  }

  public ExpressionDao function(String function) {
    this.function = function;
    return this;
  }

  public String getEntityId() {
    return entityId;
  }

  public ExpressionDao entityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public ExpressionValue getValue() {
    return value;
  }

  public ExpressionDao value(ExpressionValue value) {
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
    if (!getFunction().equals(that.getFunction())) return false;
    if (!getEntityId().equals(that.getEntityId())) return false;
    if (!getValue().equals(that.getValue())) return false;
    return getArguments().equals(that.getArguments());
  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + getFunction().hashCode();
    result = 31 * result + getEntityId().hashCode();
    result = 31 * result + getValue().hashCode();
    result = 31 * result + getArguments().hashCode();
    return result;
  }
}