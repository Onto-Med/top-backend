package care.smith.top.backend.model;

import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Category;
import care.smith.top.model.EntityType;
import care.smith.top.model.Phenotype;
import care.smith.top.model.Repository;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "entity")
public class EntityDao {
  @Enumerated
  @Column(name = "top_entity_type", nullable = false)
  private EntityType entityType;

  @Id private String id;

  @ManyToOne private RepositoryDao repository;

  @OneToMany(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private List<EntityVersionDao> versions = null;

  @OneToOne private EntityVersionDao currentVersion;

  @OneToMany(mappedBy = "origin")
  private List<EntityDao> forks = null;

  @ManyToOne private EntityDao origin;

  @ManyToMany private List<EntityDao> superEntities = null;

  @ManyToMany(mappedBy = "superEntities")
  private List<EntityDao> subEntities = null;

  public EntityDao() {}

  public EntityDao(EntityType entityType, String id) {
    this.entityType = entityType;
    this.id = id;
  }

  public EntityDao(care.smith.top.model.Entity entity) {
    id = entity.getId();
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

    Category entity;

    if (ApiModelMapper.isCategory(entityType)) entity = new Category();
    else entity = new Phenotype();

    entity
        .id(entityDao.getId())
        .entityType(entityDao.getEntityType())
        .repository(
            new Repository()
                .id(entityDao.getRepository().getId())
                .name(entityDao.getRepository().getName()));

    entity
        .author(entityVersionDao.getAuthor())
        .createdAt(entityVersionDao.getCreatedAt())
        .version(entityVersionDao.getVersion());

    if (entityVersionDao.getCodes() != null)
      entity.codes(
          entityVersionDao.getCodes().stream()
              .map(CodeDao::toApiModel)
              .collect(Collectors.toList()));
    if (entityVersionDao.getDescriptions() != null)
      entity.descriptions(
          entityVersionDao.getDescriptions().stream()
              .map(LocalisableTextDao::toApiModel)
              .collect(Collectors.toList()));
    if (entityVersionDao.getSynonyms() != null)
      entity.synonyms(
          entityVersionDao.getSynonyms().stream()
              .map(LocalisableTextDao::toApiModel)
              .collect(Collectors.toList()));
    if (entityVersionDao.getTitles() != null)
      entity.titles(
          entityVersionDao.getTitles().stream()
              .map(LocalisableTextDao::toApiModel)
              .collect(Collectors.toList()));

    if (ApiModelMapper.isAbstract(entityDao.getEntityType())) {
      ((Phenotype) entity)
          .dataType(entityVersionDao.getDataType())
          .itemType(entityVersionDao.getItemType())
          .unit(entityVersionDao.getUnit());
      if (entityVersionDao.getExpression() != null)
        ((Phenotype) entity).expression(entityVersionDao.getExpression().toApiModel());
    } else if (ApiModelMapper.isRestricted(entityDao.getEntityType())
        && entityVersionDao.getRestriction() != null)
      ((Phenotype) entity).restriction(entityVersionDao.getRestriction().toApiModel());

    if (entityDao.getSuperEntities() != null) {
      if (ApiModelMapper.isRestricted(entityDao.getEntityType())) {
        EntityDao superPhenotype = entityDao.getSuperEntities().stream().findFirst().orElse(null);
        if (superPhenotype != null)
          ((Phenotype) entity)
              .superPhenotype(
                  ((Phenotype)
                      new Phenotype()
                          .id(superPhenotype.getId())
                          .entityType(superPhenotype.getEntityType())))
              .dataType(superPhenotype.currentVersion.getDataType());
      } else {
        entity.setSuperCategories(
            entityDao.getSuperEntities().stream()
                .map(c -> ((Category) new Category().id(c.getId()).entityType(c.getEntityType())))
                .collect(Collectors.toList()));
      }
    }

    return entity;
  }

  public EntityDao removeSuperEntitiesItem(EntityDao superEntiriesItem) {
    if (superEntities != null) superEntities.remove(superEntiriesItem);
    return this;
  }

  public EntityDao addAllSuperEntitiesItems(List<EntityDao> superEntitiesItems) {
    if (superEntities == null) superEntities = new ArrayList<>();
    superEntities.addAll(superEntitiesItems);
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
    return getRepository() != null
        ? getRepository().equals(entityDao.getRepository())
        : entityDao.getRepository() == null;
  }

  @Override
  public int hashCode() {
    int result = getEntityType().hashCode();
    result = 31 * result + getId().hashCode();
    result = 31 * result + (getRepository() != null ? getRepository().hashCode() : 0);
    return result;
  }
}
