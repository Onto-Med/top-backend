package care.smith.top.backend.model;

import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Category;
import care.smith.top.model.EntityType;
import care.smith.top.model.Phenotype;
import care.smith.top.model.Repository;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

  @OneToOne private EntityVersionDao currentVersion;

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

  public EntityDao addSuperEntitiesItem(EntityDao superEntitiesItem) {
    if (superEntities == null) superEntities = new ArrayList<>();
    superEntities.add(superEntitiesItem);
    return this;
  }

  public care.smith.top.model.Entity toApiModel() {
    return toApiModel(currentVersion);
  }

  public care.smith.top.model.Entity toApiModel(EntityVersionDao entityVersionDao) {
    if (entityVersionDao == null) return new care.smith.top.model.Entity();
    EntityDao entityDao = entityVersionDao.getEntity();

    care.smith.top.model.Entity entity =
        new care.smith.top.model.Entity()
            .id(entityDao.getId())
            .entityType(entityDao.getEntityType())
            .index(entityDao.getIndex())
            .repository(
                new Repository()
                    .id(entityDao.getRepository().getId())
                    .name(entityDao.getRepository().getName()));

    entity
        .author(entityVersionDao.getAuthor())
        .codes(entityVersionDao.getCodes())
        .createdAt(entityVersionDao.getCreatedAt())
        .version(entityVersionDao.getVersion())
        .descriptions(
            entityVersionDao.getDescriptions().stream()
                .map(LocalisableTextDao::toApiModel)
                .collect(Collectors.toList()))
        .synonyms(
            entityVersionDao.getSynonyms().stream()
                .map(LocalisableTextDao::toApiModel)
                .collect(Collectors.toList()))
        .titles(
            entityVersionDao.getTitles().stream()
                .map(LocalisableTextDao::toApiModel)
                .collect(Collectors.toList()));
    if (ApiModelMapper.isAbstract(entityDao.getEntityType()))
      ((Phenotype) entity)
          .dataType(entityVersionDao.getDataType())
          .expression(entityVersionDao.getExpression().toApiModel())
          .itemType(entityVersionDao.getItemType())
          .unit(entityVersionDao.getUnit());
    if (ApiModelMapper.isRestricted(entityDao.getEntityType())
        && ((Phenotype) entity).getRestriction() != null)
      ((Phenotype) entity).restriction(entityVersionDao.getRestriction().toApiModel());

    if (entityDao.getSubEntities() != null) {
      if (ApiModelMapper.isRestricted(entityDao.getEntityType())) {
        ((Phenotype) entity)
            .setSuperPhenotype(
                entityDao.getSuperEntities().stream()
                    .findFirst()
                    .map(p -> new Phenotype().id(p.getId()))
                    .orElse(null));
      } else {
        ((Category) entity)
            .setSuperCategories(
                entityDao.getSuperEntities().stream()
                    .map(c -> new Category().id(c.getId()))
                    .collect(Collectors.toList()));
      }
    }

    return entity;
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

  public List<EntityDao> getSuperEntities() {
    return superEntities;
  }

  public EntityDao superEntities(List<EntityDao> superEntities) {
    this.superEntities = superEntities;
    return this;
  }

  public List<EntityDao> getSubEntities() {
    return subEntities;
  }

  public EntityDao subEntities(List<EntityDao> subEntities) {
    this.subEntities = subEntities;
    return this;
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
