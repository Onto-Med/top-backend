package care.smith.top.backend.util;

import care.smith.top.model.*;
import org.springframework.data.domain.Page;

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

  /**
   * This method builds a new expression from {@code expression} by mapping all entity IDs in the
   * given expression to new IDs. New IDs should be provided as values in the {@code ids} {@link
   * Map}.
   *
   * <p>If an entity ID in the expression is not contained in the keyset of {@code ids}, it is
   * removed.
   *
   * @param expression The original expression.
   * @param ids The {@link Map} containing old IDs as key and new IDs as values.
   * @return The resulting expression.
   */
  public static Expression replaceEntityIds(Expression expression, Map<String, String> ids) {
    if (expression == null) return null;
    Expression newExpression =
        new Expression()
            .functionId(expression.getFunctionId())
            .constantId(expression.getConstantId());
    if (expression.getEntityId() != null) {
      String newEntityId = ids.get(expression.getEntityId());
      if (newEntityId != null) newExpression.entityId(newEntityId);
    }
    if (expression.getArguments() != null) {
      newExpression.arguments(
          expression.getArguments().stream()
              .map(a -> replaceEntityIds(a, ids))
              .collect(Collectors.toList()));
    }
    if (expression.getValues() != null) {
      newExpression.values(
          expression.getValues().stream().map(ApiModelMapper::clone).collect(Collectors.toList()));
    }
    return newExpression;
  }

  public static EntityPage toEntityPage(Page<Entity> page) {
    return (EntityPage)
        new EntityPage()
            .content(page.getContent())
            .type("entity")
            .number(page.getNumber() + 1)
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages());
  }

  public static OrganisationPage toOrganisationPage(Page<Organisation> page) {
    return (OrganisationPage)
        new OrganisationPage()
            .content(page.getContent())
            .type("organisation")
            .number(page.getNumber() + 1)
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages());
  }

  public static QueryPage toQueryPage(Page<Query> page) {
    return (QueryPage)
        new QueryPage()
            .content(page.getContent())
            .type("query")
            .number(page.getNumber() + 1)
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages());
  }

  public static RepositoryPage toRepositoryPage(Page<Repository> page) {
    return (RepositoryPage)
        new RepositoryPage()
            .content(page.getContent())
            .type("repository")
            .number(page.getNumber() + 1)
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages());
  }

  public static UserPage toUserPage(Page<User> page) {
    return (UserPage)
        new UserPage()
            .content(page.getContent())
            .type("user")
            .number(page.getNumber() + 1)
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages());
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

  public static boolean isComposite(Entity entity) {
    return Arrays.asList(
            EntityType.COMPOSITE_PHENOTYPE,
            EntityType.COMPOSITE_RESTRICTION,
            EntityType.COMPOSITE_CONCEPT)
        .contains(entity.getEntityType());
  }

  public static int compare(Entity a, Entity b) {
    if ((a == null || a.getEntityType() == null))
      return b == null || b.getEntityType() == null ? 0 : -1;
    if (b == null || b.getEntityType() == null) return 1;

    EntityType aType = a.getEntityType();
    EntityType bType = b.getEntityType();

    if (isCategory(a) && !isCategory(b)) return -1;
    if (!isCategory(a) && isCategory(b)) return 1;

    if (isPhenotype(a) && isPhenotype(b)) {
      if (aType == EntityType.SINGLE_PHENOTYPE && isRestricted(b)) return -1;
      if (isRestricted(a) && bType == EntityType.SINGLE_PHENOTYPE) return 1;

      if (!isComposite(a) && isComposite(b)) return -1;
      if (isComposite(a) && !isComposite(b)) return 1;

      if (isComposite(a) && isComposite(b)) {
        if (expressionContains(((Phenotype) a).getExpression(), b)
            || expressionContains(
                ((Phenotype) a).getExpression(), ((Phenotype) b).getSuperPhenotype())) return 1;
        if (expressionContains(((Phenotype) b).getExpression(), a)
            || expressionContains(
                ((Phenotype) b).getExpression(), ((Phenotype) a).getSuperPhenotype())) return -1;
        if (isAbstract(a) && !isAbstract(b)) return -1;
        if (!isAbstract(a) && isAbstract(b)) return 1;
      }
    }
    return a.getId().compareTo(b.getId());
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

  public static boolean expressionContains(Expression expression, Entity entity) {
    if (expression == null || entity == null) return false;
    return getEntityIdsFromExpression(expression).contains(entity.getId());
  }
}
