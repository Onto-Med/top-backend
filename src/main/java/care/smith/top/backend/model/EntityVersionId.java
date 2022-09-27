package care.smith.top.backend.model;

public class EntityVersionId {
  private EntityDao entity;
  private Integer version;

  public EntityVersionId(EntityDao entity, Integer version) {
    this.entity  = entity;
    this.version = version;
  }
}
