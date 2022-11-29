package care.smith.top.backend.util;

import care.smith.top.model.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ApiModelMapper {
  public static Entity getEntity(List<Entity> entities, String id) {
    return entities.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
  }

  public static Set<String> getEntityIdsFromExpression(Expression expression) {
    if (expression == null) return Collections.emptySet();
    if (expression.getEntityId() != null) return Collections.singleton(expression.getEntityId());
    if (expression.getArguments() == null) return Collections.emptySet();
    return expression.getArguments().stream()
        .flatMap(a -> getEntityIdsFromExpression(a).stream())
        .collect(Collectors.toSet());
  }

  public static Expression replaceEntityIds(Expression expression, Map<String, String> ids) {
    if (expression == null) return null;
    Expression newExpression =
        new Expression()
            .functionId(expression.getFunctionId())
            .constantId(expression.getConstantId())
            .value(clone(expression.getValue()));
    if (expression.getEntityId() != null) {
      String newEntityId = ids.get(expression.getEntityId());
      if (newEntityId != null) newExpression.entityId(newEntityId);
    } else if (expression.getArguments() != null) {
      newExpression.arguments(
          expression.getArguments().stream()
              .map(a -> replaceEntityIds(a, ids))
              .collect(Collectors.toList()));
    }
    return newExpression;
  }

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

  public static List<EntityType> phenotypeTypes() {
    return Arrays.asList(
        EntityType.COMPOSITE_PHENOTYPE,
        EntityType.COMPOSITE_RESTRICTION,
        EntityType.SINGLE_PHENOTYPE,
        EntityType.SINGLE_RESTRICTION);
  }

  public static boolean isRestricted(Entity entity) {
    return isRestricted(entity.getEntityType());
  }

  public static int compareByEntityType(Entity a, Entity b) {
    if ((a == null || a.getEntityType() == null))
      return b == null || b.getEntityType() == null ? 0 : -1;
    if (b == null || b.getEntityType() == null) return 1;
    if (a.getEntityType().equals(b.getEntityType())) return 0;
    if (isCategory(a)) return -1;
    if (isCategory(b)) return 1;
    return isAbstract(a) ? -1 : 1;
  }

  public static Value clone(Value value) {
    if (value instanceof StringValue)
      return new StringValue()
          .value(((StringValue) value).getValue())
          .dataType(value.getDataType());
    if (value instanceof NumberValue)
      return new NumberValue()
          .value(((NumberValue) value).getValue())
          .dataType(value.getDataType());
    if (value instanceof BooleanValue)
      return new BooleanValue()
          .value(((BooleanValue) value).isValue())
          .dataType(value.getDataType());
    if (value instanceof DateTimeValue)
      return new DateTimeValue()
          .value(((DateTimeValue) value).getValue())
          .dataType(value.getDataType());
    return null;
  }
}
