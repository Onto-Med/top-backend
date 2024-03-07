package care.smith.top.backend.util;

import care.smith.top.model.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

public abstract class ApiModelMapper {
  public static Entity getEntity(List<Entity> entities, String id) {
    return entities.stream().filter(e -> Objects.equals(e.getId(), id)).findFirst().orElse(null);
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
        setPageMetadata(new EntityPage().content(page.getContent()), page, "entity");
  }

  public static OrganisationPage toOrganisationPage(Page<Organisation> page) {
    return (OrganisationPage)
        setPageMetadata(new OrganisationPage().content(page.getContent()), page, "organisation");
  }

  public static QueryPage toQueryPage(Page<Query> page) {
    return (QueryPage) setPageMetadata(new QueryPage().content(page.getContent()), page, "query");
  }

  public static RepositoryPage toRepositoryPage(Page<Repository> page) {
    return (RepositoryPage)
        setPageMetadata(new RepositoryPage().content(page.getContent()), page, "repository");
  }

  public static UserPage toUserPage(Page<User> page) {
    return (UserPage) setPageMetadata(new UserPage().content(page.getContent()), page, "user");
  }

  public static DocumentPage toDocumentPage(Page<Document> page) {
    return (DocumentPage)
        setPageMetadata(new DocumentPage().content(page.getContent()), page, "document");
  }

  public static ConceptClusterPage toConceptClusterPage(Page<ConceptCluster> page) {
    return (ConceptClusterPage)
        setPageMetadata(
            new ConceptClusterPage().content(page.getContent()), page, "conceptCluster");
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

  public static boolean isConcept(EntityType entityType) {
    return EntityType.SINGLE_CONCEPT.equals(entityType)
        || EntityType.COMPOSITE_CONCEPT.equals(entityType);
  }

  public static boolean isConcept(Entity entity) {
    return isConcept(entity.getEntityType());
  }

  public static boolean isCompositeConcept(EntityType entityType) {
    return EntityType.COMPOSITE_CONCEPT == entityType;
  }

  public static boolean isCompositeConcept(Entity entity) {
    return isCompositeConcept(entity.getEntityType());
  }

  public static boolean isSingleConcept(EntityType entityType) {
    return EntityType.SINGLE_CONCEPT == entityType;
  }

  public static boolean isSingleConcept(Entity entity) {
    return isSingleConcept(entity.getEntityType());
  }

  public static boolean isRestricted(EntityType entityType) {
    return Arrays.asList(EntityType.SINGLE_RESTRICTION, EntityType.COMPOSITE_RESTRICTION)
        .contains(entityType);
  }

  public static boolean canHaveSubs(EntityType entityType) {
    return isCategory(entityType) || isSingleConcept(entityType);
  }

  public static List<EntityType> phenotypeTypes() {
    return Arrays.asList(
        EntityType.COMPOSITE_PHENOTYPE,
        EntityType.COMPOSITE_RESTRICTION,
        EntityType.SINGLE_PHENOTYPE,
        EntityType.SINGLE_RESTRICTION);
  }

  public static List<EntityType> conceptTypes() {
    return Arrays.asList(EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT);
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

    if (isCategory(a) && isCategory(b)) {
      if (((Category) a).getSuperCategories() != null
          && ((Category) a)
              .getSuperCategories().stream().anyMatch(c -> c.getId().equals(b.getId()))) return 1;
      if (((Category) b).getSuperCategories() != null
          && ((Category) b)
              .getSuperCategories().stream().anyMatch(c -> c.getId().equals(a.getId()))) return -1;
    }

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

    if (isConcept(a) && isConcept(b)) {
      if (((Concept) a).getSuperConcepts() != null
          && ((Concept) a).getSuperConcepts().stream().anyMatch(c -> c.getId().equals(b.getId())))
        return 1;
      if (((Concept) b).getSuperConcepts() != null
          && ((Concept) b).getSuperConcepts().stream().anyMatch(c -> c.getId().equals(a.getId())))
        return -1;
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

  private static <T> care.smith.top.model.Page setPageMetadata(
      @NotNull care.smith.top.model.Page apiPage, @NotNull Page<T> page, @NotNull String type) {
    return apiPage
        .type(type)
        .number(page.getNumber() + 1)
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages());
  }
}
