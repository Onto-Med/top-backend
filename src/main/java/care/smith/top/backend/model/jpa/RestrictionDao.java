package care.smith.top.backend.model.jpa;

import care.smith.top.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

@Entity(name = "restriction")
public class RestrictionDao {
  @Id @GeneratedValue private Long id;

  @Column(nullable = false)
  private DataType dataType;

  private Quantifier quantifier;

  private Integer cardinality;

  @ElementCollection private List<String> stringValues;
  @ElementCollection private List<BigDecimal> numberValues;
  @ElementCollection private List<LocalDateTime> dateTimeValues;
  @ElementCollection private List<Boolean> booleanValues;
  private RestrictionOperator minOperator;
  private RestrictionOperator maxOperator;

  private LocalDateTime minimumDateTimeValue;
  private LocalDateTime maximumDateTimeValue;
  private BigDecimal minimumNumberValue;
  private BigDecimal maximumNumberValue;

  public RestrictionDao() {}

  public RestrictionDao(@NotNull Restriction restriction) {
    dataType = restriction.getType();
    quantifier = restriction.getQuantifier();
    cardinality = restriction.getCardinality();
    if (restriction instanceof BooleanRestriction)
      booleanValues = ((BooleanRestriction) restriction).getValues();
    if (restriction instanceof StringRestriction)
      stringValues = ((StringRestriction) restriction).getValues();
    if (restriction instanceof DateTimeRestriction) {
      List<LocalDateTime> values = ((DateTimeRestriction) restriction).getValues();
      RestrictionOperator minOperator = ((DateTimeRestriction) restriction).getMinOperator();
      RestrictionOperator maxOperator = ((DateTimeRestriction) restriction).getMaxOperator();
      if (minOperator != null || maxOperator != null) {
        if (values != null && !values.isEmpty() && values.get(0) != null) {
          minimumDateTimeValue = values.get(0);
          this.minOperator = minOperator;
        }
        if (values != null && values.size() > 1 && values.get(1) != null) {
          maximumDateTimeValue = values.get(1);
          this.maxOperator = maxOperator;
        }
      } else {
        dateTimeValues = values;
      }
    }
    if (restriction instanceof NumberRestriction) {
      List<BigDecimal> values = ((NumberRestriction) restriction).getValues();
      RestrictionOperator minOperator = ((NumberRestriction) restriction).getMinOperator();
      RestrictionOperator maxOperator = ((NumberRestriction) restriction).getMaxOperator();
      if (minOperator != null || maxOperator != null) {
        if (values != null && !values.isEmpty() && values.get(0) != null) {
          minimumNumberValue = values.get(0);
          this.minOperator = minOperator;
        }
        if (values != null && values.size() > 1 && values.get(1) != null) {
          maximumNumberValue = values.get(1);
          this.maxOperator = maxOperator;
        }
      } else {
        numberValues = values;
      }
    }
  }

  public <T> RestrictionDao(
      @NotNull DataType dataType, Quantifier quantifier, Integer cardinality, List<T> values) {
    this.dataType = dataType;
    this.quantifier = quantifier;
    this.cardinality = cardinality;
    if (values != null && !values.isEmpty()) {
      if (values.get(0) instanceof String) stringValues = (List<String>) values;
      if (values.get(0) instanceof Boolean) booleanValues = (List<Boolean>) values;
      if (values.get(0) instanceof BigDecimal) numberValues = (List<BigDecimal>) values;
      if (values.get(0) instanceof LocalDateTime) dateTimeValues = (List<LocalDateTime>) values;
    }
  }

  public RestrictionDao(
      @NotNull DataType dataType,
      Quantifier quantifier,
      Integer cardinality,
      LocalDateTime min,
      LocalDateTime max,
      RestrictionOperator minOperator,
      RestrictionOperator maxOperator) {
    this.dataType = dataType;
    this.quantifier = quantifier;
    this.cardinality = cardinality;
    this.minimumDateTimeValue = min;
    this.maximumDateTimeValue = max;
    this.minOperator = minOperator;
    this.maxOperator = maxOperator;
  }

  public RestrictionDao(
      @NotNull DataType dataType,
      Quantifier quantifier,
      Integer cardinality,
      BigDecimal min,
      BigDecimal max,
      RestrictionOperator minOperator,
      RestrictionOperator maxOperator) {
    this.dataType = dataType;
    this.quantifier = quantifier;
    this.cardinality = cardinality;
    this.minimumNumberValue = min;
    this.maximumNumberValue = max;
    this.minOperator = minOperator;
    this.maxOperator = maxOperator;
  }

  public RestrictionDao id(@NotNull Long id) {
    this.id = id;
    return this;
  }

  public RestrictionDao dataType(@NotNull DataType dataType) {
    this.dataType = dataType;
    return this;
  }

  public RestrictionDao quantifier(Quantifier quantifier) {
    this.quantifier = quantifier;
    return this;
  }

  public RestrictionDao cardinality(Integer cardinality) {
    this.cardinality = cardinality;
    return this;
  }

  public RestrictionDao stringValues(List<String> stringValues) {
    this.stringValues = stringValues;
    return this;
  }

  public RestrictionDao numberValues(List<BigDecimal> numberValues) {
    this.numberValues = numberValues;
    return this;
  }

  public RestrictionDao dateTimeValues(List<LocalDateTime> dateTimeValues) {
    this.dateTimeValues = dateTimeValues;
    return this;
  }

  public RestrictionDao booleanValues(List<Boolean> booleanValues) {
    this.booleanValues = booleanValues;
    return this;
  }

  public RestrictionDao minOperator(RestrictionOperator minOperator) {
    this.minOperator = minOperator;
    return this;
  }

  public RestrictionDao maxOperator(RestrictionOperator maxOperator) {
    this.maxOperator = maxOperator;
    return this;
  }

  public RestrictionDao minimumDateTimeValue(LocalDateTime minimumDateTimeValue) {
    this.minimumDateTimeValue = minimumDateTimeValue;
    return this;
  }

  public RestrictionDao maximumDateTimeValue(LocalDateTime maximumDateTimeValue) {
    this.maximumDateTimeValue = maximumDateTimeValue;
    return this;
  }

  public RestrictionDao minimumNumberValue(BigDecimal minimumNumberValue) {
    this.minimumNumberValue = minimumNumberValue;
    return this;
  }

  public RestrictionDao maximumNumberValue(BigDecimal maximumNumberValue) {
    this.maximumNumberValue = maximumNumberValue;
    return this;
  }

  public <T> RestrictionDao addValuesItem(T valuesItem) {
    if (valuesItem instanceof Boolean) return addBooleanValuesItem((Boolean) valuesItem);
    if (valuesItem instanceof LocalDateTime)
      return addDateTimeValuesItem((LocalDateTime) valuesItem);
    if (valuesItem instanceof BigDecimal) return addNumberValuesItem((BigDecimal) valuesItem);
    if (valuesItem instanceof String) return addStringValuesItem((String) valuesItem);
    return this;
  }

  public RestrictionDao addBooleanValuesItem(Boolean booleanValuesItem) {
    if (booleanValues == null) booleanValues = new ArrayList<>();
    booleanValues.add(booleanValuesItem);
    return this;
  }

  public RestrictionDao addDateTimeValuesItem(LocalDateTime dateTimeValuesItem) {
    if (dateTimeValues == null) dateTimeValues = new ArrayList<>();
    dateTimeValues.add(dateTimeValuesItem);
    return this;
  }

  public RestrictionDao addNumberValuesItem(BigDecimal numberValuesItem) {
    if (numberValues == null) numberValues = new ArrayList<>();
    numberValues.add(numberValuesItem);
    return this;
  }

  public RestrictionDao addStringValuesItem(String stringValuesItem) {
    if (stringValues == null) stringValues = new ArrayList<>();
    stringValues.add(stringValuesItem);
    return this;
  }

  public Restriction toApiModel() {
    Restriction restriction;
    if (DataType.BOOLEAN.equals(dataType)) {
      restriction = new BooleanRestriction().values(getBooleanValues());
    } else if (DataType.DATE_TIME.equals(dataType)) {
      restriction = new DateTimeRestriction().minOperator(minOperator).maxOperator(maxOperator);
      if (minOperator != null || maxOperator != null)
        ((DateTimeRestriction) restriction)
            .addValuesItem(minimumDateTimeValue)
            .addValuesItem(maximumDateTimeValue);
      else ((DateTimeRestriction) restriction).values(getDateTimeValues());
    } else if (DataType.NUMBER.equals(dataType)) {
      restriction = new NumberRestriction().minOperator(minOperator).maxOperator(maxOperator);
      if (minOperator != null || maxOperator != null)
        ((NumberRestriction) restriction)
            .addValuesItem(minimumNumberValue)
            .addValuesItem(maximumNumberValue);
      else ((NumberRestriction) restriction).values(getNumberValues());
    } else if (DataType.STRING.equals(dataType)) {
      restriction = new StringRestriction().values(getStringValues());
    } else {
      restriction = new Restriction();
    }
    return restriction.quantifier(quantifier).cardinality(cardinality).type(dataType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RestrictionDao that = (RestrictionDao) o;

    if (getDataType() != that.getDataType()) return false;
    if (getQuantifier() != that.getQuantifier()) return false;
    if (getCardinality() != null
        ? !getCardinality().equals(that.getCardinality())
        : that.getCardinality() != null) return false;
    if (getStringValues() != null
        ? !getStringValues().equals(that.getStringValues())
        : that.getStringValues() != null) return false;
    if (getNumberValues() != null
        ? !getNumberValues().equals(that.getNumberValues())
        : that.getNumberValues() != null) return false;
    if (getDateTimeValues() != null
        ? !getDateTimeValues().equals(that.getDateTimeValues())
        : that.getDateTimeValues() != null) return false;
    if (getBooleanValues() != null
        ? !getBooleanValues().equals(that.getBooleanValues())
        : that.getBooleanValues() != null) return false;
    if (getMinOperator() != that.getMinOperator()) return false;
    if (getMaxOperator() != that.getMaxOperator()) return false;
    if (getMinimumDateTimeValue() != null
        ? !getMinimumDateTimeValue().equals(that.getMinimumDateTimeValue())
        : that.getMinimumDateTimeValue() != null) return false;
    if (getMaximumDateTimeValue() != null
        ? !getMaximumDateTimeValue().equals(that.getMaximumDateTimeValue())
        : that.getMaximumDateTimeValue() != null) return false;
    if (getMinimumNumberValue() != null
        ? !getMinimumNumberValue().equals(that.getMinimumNumberValue())
        : that.getMinimumNumberValue() != null) return false;
    return getMaximumNumberValue() != null
        ? getMaximumNumberValue().equals(that.getMaximumNumberValue())
        : that.getMaximumNumberValue() == null;
  }

  @Override
  public int hashCode() {
    int result = getDataType() != null ? getDataType().hashCode() : 0;
    result = 31 * result + (getQuantifier() != null ? getQuantifier().hashCode() : 0);
    result = 31 * result + (getCardinality() != null ? getCardinality().hashCode() : 0);
    result = 31 * result + (getStringValues() != null ? getStringValues().hashCode() : 0);
    result = 31 * result + (getNumberValues() != null ? getNumberValues().hashCode() : 0);
    result = 31 * result + (getDateTimeValues() != null ? getDateTimeValues().hashCode() : 0);
    result = 31 * result + (getBooleanValues() != null ? getBooleanValues().hashCode() : 0);
    result = 31 * result + (getMinOperator() != null ? getMinOperator().hashCode() : 0);
    result = 31 * result + (getMaxOperator() != null ? getMaxOperator().hashCode() : 0);
    result =
        31 * result
            + (getMinimumDateTimeValue() != null ? getMinimumDateTimeValue().hashCode() : 0);
    result =
        31 * result
            + (getMaximumDateTimeValue() != null ? getMaximumDateTimeValue().hashCode() : 0);
    result =
        31 * result + (getMinimumNumberValue() != null ? getMinimumNumberValue().hashCode() : 0);
    result =
        31 * result + (getMaximumNumberValue() != null ? getMaximumNumberValue().hashCode() : 0);
    return result;
  }

  public LocalDateTime getMinimumDateTimeValue() {
    return minimumDateTimeValue;
  }

  public LocalDateTime getMaximumDateTimeValue() {
    return maximumDateTimeValue;
  }

  public BigDecimal getMinimumNumberValue() {
    return minimumNumberValue;
  }

  public BigDecimal getMaximumNumberValue() {
    return maximumNumberValue;
  }

  public Long getId() {
    return id;
  }

  public DataType getDataType() {
    return dataType;
  }

  public Quantifier getQuantifier() {
    return quantifier;
  }

  public Integer getCardinality() {
    return cardinality;
  }

  public List<String> getStringValues() {
    return stringValues == null ? null : new ArrayList<>(stringValues);
  }

  public List<BigDecimal> getNumberValues() {
    return numberValues == null ? null : new ArrayList<>(numberValues);
  }

  public List<LocalDateTime> getDateTimeValues() {
    return dateTimeValues == null ? null : new ArrayList<>(dateTimeValues);
  }

  public List<Boolean> getBooleanValues() {
    return booleanValues == null ? null : new ArrayList<>(booleanValues);
  }

  public RestrictionOperator getMinOperator() {
    return minOperator;
  }

  public RestrictionOperator getMaxOperator() {
    return maxOperator;
  }
}
