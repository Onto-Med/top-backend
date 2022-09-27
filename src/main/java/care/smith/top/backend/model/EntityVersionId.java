package care.smith.top.backend.model;

import java.io.Serializable;

public class EntityVersionId implements Serializable {
  private EntityDao entity;
  private Integer version;

  public EntityVersionId(EntityDao entity, Integer version) {
    this.entity  = entity;
    this.version = version;
  }
}
