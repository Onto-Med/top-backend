package care.smith.top.backend.model;

import care.smith.top.model.EntityType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.List;

@Entity(name = "entity")
@EntityListeners(AuditingEntityListener.class)
public class EntityDao {
  @Column(name = "top_entity_type")
  private EntityType entityType;

  @Id private String id;

  private Integer index;

  @ManyToOne private RepositoryDao repository;

  @OneToMany(mappedBy = "entity")
  private List<EntityVersionDao> versions = null;

  @OneToOne
  private EntityVersionDao currentVersion;

  @OneToMany(mappedBy = "origin")
  private List<EntityDao> forks = null;

  @ManyToOne private EntityDao origin;

  @ManyToMany private List<EntityDao> superEntities = null;

  @ManyToMany(mappedBy = "superEntities")
  private List<EntityDao> subEntities = null;

  public EntityDao() {}

  public EntityDao(EntityType entityType, String id, Integer index) {
    this.entityType = entityType;
    this.id = id;
    this.index = index;
  }

  public EntityDao(care.smith.top.model.Entity entity) {
    id = entity.getId();
    index = entity.getIndex();
    entityType = entity.getEntityType();
  }

  public EntityDao currentVersion(EntityVersionDao currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }

  public EntityDao entityType(EntityType entityType) {
    this.entityType = entityType;
    return this;
  }

  public EntityDao id(String id) {
    this.id = id;
    return this;
  }

  public EntityDao index(Integer index) {
    this.index = index;
    return this;
  }

  public EntityDao repository(RepositoryDao repository) {
    this.repository = repository;
    return this;
  }

  public EntityDao versions(List<EntityVersionDao> versions) {
    this.versions = versions;
    return this;
  }

  public EntityDao forks(List<EntityDao> forks) {
    this.forks = forks;
    return this;
  }

  public EntityDao origin(EntityDao origin) {
    this.origin = origin;
    return this;
  }

  public EntityVersionDao getCurrentVersion() {
    return currentVersion;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public String getId() {
    return id;
  }

  public Integer getIndex() {
    return index;
  }

  public RepositoryDao getRepository() {
    return repository;
  }

  public List<EntityVersionDao> getVersions() {
    return versions;
  }

  public List<EntityDao> getForks() {
    return forks;
  }

  public EntityDao getOrigin() {
    return origin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EntityDao entityDao = (EntityDao) o;

    if (getEntityType() != entityDao.getEntityType()) return false;
    if (!getId().equals(entityDao.getId())) return false;
    if (getIndex() != null
      ? !getIndex().equals(entityDao.getIndex())
      : entityDao.getIndex() != null) return false;
    return getRepository() != null
      ? getRepository().equals(entityDao.getRepository())
      : entityDao.getRepository() == null;
  }

  @Override
  public int hashCode() {
    int result = getEntityType().hashCode();
    result = 31 * result + getId().hashCode();
    result = 31 * result + (getIndex() != null ? getIndex().hashCode() : 0);
    result = 31 * result + (getRepository() != null ? getRepository().hashCode() : 0);
    return result;
  }
}
