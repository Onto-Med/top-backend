package care.smith.top.backend.util;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.EntityType;

import java.util.Arrays;
import java.util.List;

public abstract class ApiModelMapper {
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
}
