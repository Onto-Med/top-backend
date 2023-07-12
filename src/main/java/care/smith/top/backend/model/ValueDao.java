package care.smith.top.backend.model;

import care.smith.top.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "value")
public class ValueDao {
  @Id @GeneratedValue private Long id;

  private Boolean booleanValue;
  private LocalDateTime dateTimeValue;
  private BigDecimal numberValue;
  private String stringValue;

  public ValueDao() {}

  public ValueDao(Value value) {
    if (value != null) {
      if (value instanceof BooleanValue) booleanValue = ((BooleanValue) value).isValue();
      if (value instanceof DateTimeValue) dateTimeValue = ((DateTimeValue) value).getValue();
      if (value instanceof NumberValue) numberValue = ((NumberValue) value).getValue();
      if (value instanceof StringValue) stringValue = ((StringValue) value).getValue();
    }
  }

  public ValueDao(Boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public ValueDao(LocalDateTime dateTimeValue) {
    this.dateTimeValue = dateTimeValue;
  }

  public ValueDao(BigDecimal numberValue) {
    this.numberValue = numberValue;
  }

  public ValueDao(String stringValue) {
    this.stringValue = stringValue;
  }

  public Value toApiModel() {
    if (booleanValue != null)
      return new BooleanValue().value(booleanValue).dataType(DataType.BOOLEAN);
    if (dateTimeValue != null)
      return new DateTimeValue().value(dateTimeValue).dataType(DataType.DATE_TIME);
    if (numberValue != null) return new NumberValue().value(numberValue).dataType(DataType.NUMBER);
    if (stringValue != null) return new StringValue().value(stringValue).dataType(DataType.STRING);
    return null;
  }
}
