package care.smith.top.backend.model;

import care.smith.top.model.*;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "restriction")
public class RestrictionDao {
  @Id private Long id;
  private DataType dataType;
  private Quantifier quantifier;
  private Integer cardinality;
  @ElementCollection private List<String> stringValues;
  @ElementCollection private List<BigDecimal> numberValues;
  @ElementCollection private List<LocalDateTime> dateTimeValues;
  @ElementCollection private List<Boolean> booleanValues;
  private RestrictionOperator minOperator;
  private RestrictionOperator maxOperator;

  public RestrictionDao() {}

  public RestrictionDao(Restriction restriction) {
    dataType = restriction.getType();
    quantifier = restriction.getQuantifier();
    cardinality = restriction.getCardinality();
    if (restriction instanceof BooleanRestriction)
      booleanValues = ((BooleanRestriction) restriction).getValues();
    if (restriction instanceof StringRestriction)
      stringValues = ((StringRestriction) restriction).getValues();
    if (restriction instanceof DateTimeRestriction) {
      dateTimeValues = ((DateTimeRestriction) restriction).getValues();
      minOperator = ((DateTimeRestriction) restriction).getMinOperator();
      maxOperator = ((DateTimeRestriction) restriction).getMaxOperator();
    }
    if (restriction instanceof NumberRestriction) {
      numberValues = ((NumberRestriction) restriction).getValues();
      minOperator = ((NumberRestriction) restriction).getMinOperator();
      maxOperator = ((NumberRestriction) restriction).getMaxOperator();
    }
  }

  public <T> RestrictionDao(
      DataType dataType, Quantifier quantifier, Integer cardinality, List<T> values) {
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
      DataType dataType,
      Quantifier quantifier,
      Integer cardinality,
      LocalDateTime min,
      LocalDateTime max,
      RestrictionOperator minOperator,
      RestrictionOperator maxOperator) {
    this.dataType = dataType;
    this.quantifier = quantifier;
    this.cardinality = cardinality;
    addValuesItem(min);
    addValuesItem(max);
    this.minOperator = minOperator;
    this.maxOperator = maxOperator;
  }

  public RestrictionDao(
      DataType dataType,
      Quantifier quantifier,
      Integer cardinality,
      BigDecimal min,
      BigDecimal max,
      RestrictionOperator minOperator,
      RestrictionOperator maxOperator) {
    this.dataType = dataType;
    this.quantifier = quantifier;
    this.cardinality = cardinality;
    addValuesItem(min);
    addValuesItem(max);
    this.minOperator = minOperator;
    this.maxOperator = maxOperator;
  }

  public RestrictionDao id(Long id) {
    this.id = id;
    return this;
  }

  public RestrictionDao dataType(DataType dataType) {
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
    return getMaxOperator() == that.getMaxOperator();
  }

  @Override
  public int hashCode() {
    int result = getDataType().hashCode();
    result = 31 * result + getQuantifier().hashCode();
    result = 31 * result + (getCardinality() != null ? getCardinality().hashCode() : 0);
    result = 31 * result + (getStringValues() != null ? getStringValues().hashCode() : 0);
    result = 31 * result + (getNumberValues() != null ? getNumberValues().hashCode() : 0);
    result = 31 * result + (getDateTimeValues() != null ? getDateTimeValues().hashCode() : 0);
    result = 31 * result + (getBooleanValues() != null ? getBooleanValues().hashCode() : 0);
    result = 31 * result + (getMinOperator() != null ? getMinOperator().hashCode() : 0);
    result = 31 * result + (getMaxOperator() != null ? getMaxOperator().hashCode() : 0);
    return result;
  }

  public Restriction toApiModel() {
    Restriction restriction;
    if (DataType.BOOLEAN.equals(dataType))
      restriction = new BooleanRestriction().values(booleanValues);
    else if (DataType.DATE_TIME.equals(dataType))
      restriction =
          new DateTimeRestriction()
              .values(dateTimeValues)
              .minOperator(minOperator)
              .maxOperator(maxOperator);
    else if (DataType.NUMBER.equals(dataType))
      restriction =
          new NumberRestriction()
              .values(numberValues)
              .minOperator(minOperator)
              .maxOperator(maxOperator);
    else if (DataType.STRING.equals(dataType))
      restriction = new StringRestriction().values(stringValues);
    else restriction = new Restriction();
    return restriction.quantifier(quantifier).cardinality(cardinality).type(dataType);
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
    return stringValues;
  }

  public List<BigDecimal> getNumberValues() {
    return numberValues;
  }

  public List<LocalDateTime> getDateTimeValues() {
    return dateTimeValues;
  }

  public List<Boolean> getBooleanValues() {
    return booleanValues;
  }

  public RestrictionOperator getMinOperator() {
    return minOperator;
  }

  public RestrictionOperator getMaxOperator() {
    return maxOperator;
  }
}
