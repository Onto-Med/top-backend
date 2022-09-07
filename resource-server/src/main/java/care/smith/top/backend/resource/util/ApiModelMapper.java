package care.smith.top.backend.resource.util;

import care.smith.top.backend.model.*;
import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.simple_onto_api.calculator.Calculator;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ApiModelMapper {
  public static final String EXPRESSION_CONSTANT_PROPERTY = "constant";
  public static final String EXPRESSION_VALUE_PROPERTY = "value";
  private static final Calculator calculator = new Calculator();

  public static EntityType toRestrictedEntityType(EntityType entityType) {
    if (EntityType.SINGLE_PHENOTYPE.equals(entityType)) return EntityType.SINGLE_RESTRICTION;
    if (EntityType.COMPOSITE_PHENOTYPE.equals(entityType)) return EntityType.COMPOSITE_RESTRICTION;
    return null;
  }

  public static boolean isAbstract(EntityType entityType) {
    return Arrays.asList(EntityType.SINGLE_PHENOTYPE, EntityType.COMPOSITE_PHENOTYPE)
        .contains(entityType);
  }

  public static boolean isAbstract(Entity entity) {
    return isAbstract(entity.getEntityType());
  }

  public static boolean isCategory(EntityType entityType) {
    return EntityType.CATEGORY.equals(entityType);
  }

  public static boolean isCategory(Entity entity) {
    return isCategory(entity.getEntityType());
  }

  public static boolean isPhenotype(EntityType entityType) {
    return ApiModelMapper.isAbstract(entityType) || ApiModelMapper.isRestricted(entityType);
  }

  public static boolean isPhenotype(Entity entity) {
    return isPhenotype(entity.getEntityType());
  }

  public static boolean isRestricted(EntityType entityType) {
    return Arrays.asList(EntityType.SINGLE_RESTRICTION, EntityType.COMPOSITE_RESTRICTION)
        .contains(entityType);
  }

  public static boolean isRestricted(Entity entity) {
    return isRestricted(entity.getEntityType());
  }

  public static Annotation toAnnotation(ExpressionValue expressionValue) {
    if (expressionValue == null) return null;

    if (expressionValue.getConstant() != null)
      return new Annotation(
          EXPRESSION_CONSTANT_PROPERTY, expressionValue.getConstant().getId(), null);

    if (expressionValue.getValue() != null) {
      Value value = expressionValue.getValue();
      if (value instanceof StringValue)
        return new Annotation(EXPRESSION_VALUE_PROPERTY, ((StringValue) value).getValue(), null);
      if (value instanceof NumberValue)
        return new Annotation(
            EXPRESSION_VALUE_PROPERTY, ((NumberValue) value).getValue().doubleValue(), null);
      if (value instanceof DateTimeValue)
        return new Annotation(
            EXPRESSION_VALUE_PROPERTY, ((DateTimeValue) value).getValue().toInstant(), null);
      if (value instanceof BooleanValue)
        return new Annotation(EXPRESSION_VALUE_PROPERTY, ((BooleanValue) value).isValue(), null);
    }

    return null;
  }

  public static Annotation toAnnotation(Code code) {
    if (code == null
        || code.getCode() == null
        || code.getCodeSystem() == null
        || code.getCodeSystem().getUri() == null) return null;

    return (Annotation)
        new Annotation("code", code.getCode(), null)
            .addAnnotation(
                new Annotation("codeSystem", code.getCodeSystem().getUri().toString(), null));
  }

  public static Annotation toAnnotation(Unit unit) {
    if (unit == null || !StringUtils.hasText(unit.getUnit())) return null;
    return new Annotation().setProperty("unit").setStringValue(unit.getUnit());
  }

  public static Code toCode(Annotation annotation) {
    if (annotation == null || !"code".equals(annotation.getProperty())) return null;

    Optional<Annotation> codeSystem = annotation.getAnnotation("codeSystem");
    if (codeSystem.isEmpty()) return null;

    try {
      return new Code()
          .code(annotation.getStringValue())
          .codeSystem(new CodeSystem().uri(new URI(codeSystem.get().getStringValue())));
    } catch (URISyntaxException e) {
      return null;
    }
  }

  public static Expression toExpression(Annotation annotation) {
    if ("class".equals(annotation.getDatatype()))
      return new Expression()
          .function("entity")
          .entityId(annotation.getClassValue() != null ? annotation.getClassValue().getId() : null);

    if (EXPRESSION_CONSTANT_PROPERTY.equals(annotation.getProperty()))
      return new Expression()
          .function("constant")
          .value(
              new ExpressionValue()
                  .constant(
                      OntoModelMapper.map(calculator.getConstant(annotation.getStringValue()))));

    if (EXPRESSION_VALUE_PROPERTY.equals(annotation.getProperty()))
      return new Expression()
          .function("value")
          .value(new ExpressionValue().value(toValue(annotation)));

    Expression expression = new Expression().function(annotation.getStringValue());
    expression.arguments(
        annotation.getSortedAnnotations().stream()
            .map(ApiModelMapper::toExpression)
            .collect(Collectors.toList()));
    return expression;
  }

  public static Unit toUnit(Annotation annotation) {
    if (annotation == null
        || !"unit".equals(annotation.getProperty())
        || !StringUtils.hasText(annotation.getStringValue())) return null;
    return new Unit().unit(annotation.getStringValue());
  }

  public static Value toValue(Annotation annotation) {
    if (annotation == null || annotation.getDatatype() == null) return null;

    if (annotation.getDatatype().equals(DataType.STRING.getValue()))
      return new StringValue().value(annotation.getStringValue()).dataType(DataType.STRING);
    if (annotation.getDatatype().equals(DataType.NUMBER.getValue()))
      return new NumberValue()
          .value(BigDecimal.valueOf(annotation.getNumberValue()))
          .dataType(DataType.NUMBER);
    if (annotation.getDatatype().equals(DataType.DATE_TIME.getValue()))
      return new DateTimeValue()
          .value(annotation.getDateValue().atOffset(ZoneOffset.UTC))
          .dataType(DataType.DATE_TIME);
    if (annotation.getDatatype().equals(DataType.BOOLEAN.getValue()))
      return new BooleanValue().value(annotation.getBooleanValue()).dataType(DataType.BOOLEAN);

    return null;
  }
}
