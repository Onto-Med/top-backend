package care.smith.top.backend.model;

import care.smith.top.model.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "entity_version")
public class EntityVersionDao {
  @Id @GeneratedValue private long id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(nullable = false)
  private EntityDao entity;

  @Column(nullable = false)
  private Integer version;

  @ElementCollection @OrderColumn private List<LocalisableTextDao> titles = null;

  @ElementCollection @OrderColumn private List<LocalisableTextDao> synonyms = null;

  @ElementCollection @OrderColumn private List<LocalisableTextDao> descriptions = null;

  @ElementCollection @OrderColumn private List<CodeDao> codes = null;

  @OneToOne private EntityVersionDao previousVersion;

  @OneToOne private EntityVersionDao nextVersion;

  @ManyToMany private List<EntityVersionDao> equivalentEntities = null;

  @CreatedBy private String author;

  private DataType dataType;

  private ItemType itemType;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private RestrictionDao restriction;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private ExpressionDao expression;

  private Unit unit;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  public EntityVersionDao() {}

  public EntityVersionDao(
      EntityDao entity,
      Integer version,
      List<LocalisableTextDao> titles,
      List<LocalisableTextDao> synonyms,
      List<LocalisableTextDao> descriptions,
      List<CodeDao> codes,
      String author,
      DataType dataType,
      ItemType itemType,
      RestrictionDao restriction,
      ExpressionDao expression,
      Unit unit) {
    this.entity = entity;
    this.version = version;
    this.titles = titles;
    this.synonyms = synonyms;
    this.descriptions = descriptions;
    this.codes = codes;
    this.author = author;
    this.dataType = dataType;
    this.itemType = itemType;
    this.restriction = restriction;
    this.expression = expression;
    this.unit = unit;
  }

  public EntityVersionDao(care.smith.top.model.Entity entity) {
    if (entity.getTitles() != null)
      titles =
          entity.getTitles().stream().map(LocalisableTextDao::new).collect(Collectors.toList());
    if (entity.getSynonyms() != null)
      synonyms =
          entity.getSynonyms().stream().map(LocalisableTextDao::new).collect(Collectors.toList());
    if (entity.getDescriptions() != null)
      descriptions =
          entity.getDescriptions().stream()
              .map(LocalisableTextDao::new)
              .collect(Collectors.toList());
    if (entity.getCodes() != null)
      codes = entity.getCodes().stream().map(CodeDao::new).collect(Collectors.toList());
    if (entity instanceof Phenotype) {
      dataType = ((Phenotype) entity).getDataType();
      itemType = ((Phenotype) entity).getItemType();
      if (restriction != null)
        restriction = new RestrictionDao(((Phenotype) entity).getRestriction());
      expression = new ExpressionDao(((Phenotype) entity).getExpression());
      unit = ((Phenotype) entity).getUnit();
    }
  }

  public EntityVersionDao dataType(DataType dataType) {
    this.dataType = dataType;
    return this;
  }

  public EntityVersionDao expression(ExpressionDao expression) {
    this.expression = expression;
    return this;
  }

  public EntityVersionDao itemType(ItemType itemType) {
    this.itemType = itemType;
    return this;
  }

  public EntityVersionDao restriction(RestrictionDao restriction) {
    this.restriction = restriction;
    return this;
  }

  public care.smith.top.model.Entity toApiModel() {
    return entity.toApiModel(this);
  }

  public EntityVersionDao unit(Unit unit) {
    this.unit = unit;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EntityVersionDao that = (EntityVersionDao) o;

    if (!getEntity().equals(that.getEntity())) return false;
    if (!getVersion().equals(that.getVersion())) return false;
    if (getTitles() != null ? !getTitles().equals(that.getTitles()) : that.getTitles() != null)
      return false;
    if (getSynonyms() != null
        ? !getSynonyms().equals(that.getSynonyms())
        : that.getSynonyms() != null) return false;
    if (getDescriptions() != null
        ? !getDescriptions().equals(that.getDescriptions())
        : that.getDescriptions() != null) return false;
    if (getCodes() != null ? !getCodes().equals(that.getCodes()) : that.getCodes() != null)
      return false;
    if (getAuthor() != null ? !getAuthor().equals(that.getAuthor()) : that.getAuthor() != null)
      return false;
    if (getDataType() != that.getDataType()) return false;
    if (getItemType() != that.getItemType()) return false;
    if (getRestriction() != null
        ? !getRestriction().equals(that.getRestriction())
        : that.getRestriction() != null) return false;
    if (getExpression() != null
        ? !getExpression().equals(that.getExpression())
        : that.getExpression() != null) return false;
    if (getUnit() != null ? !getUnit().equals(that.getUnit()) : that.getUnit() != null)
      return false;
    return getCreatedAt() != null
        ? getCreatedAt().equals(that.getCreatedAt())
        : that.getCreatedAt() == null;
  }

  @Override
  public int hashCode() {
    int result = getEntity().hashCode();
    result = 31 * result + getVersion().hashCode();
    result = 31 * result + (getTitles() != null ? getTitles().hashCode() : 0);
    result = 31 * result + (getSynonyms() != null ? getSynonyms().hashCode() : 0);
    result = 31 * result + (getDescriptions() != null ? getDescriptions().hashCode() : 0);
    result = 31 * result + (getCodes() != null ? getCodes().hashCode() : 0);
    result = 31 * result + (getAuthor() != null ? getAuthor().hashCode() : 0);
    result = 31 * result + (getDataType() != null ? getDataType().hashCode() : 0);
    result = 31 * result + (getItemType() != null ? getItemType().hashCode() : 0);
    result = 31 * result + (getRestriction() != null ? getRestriction().hashCode() : 0);
    result = 31 * result + (getExpression() != null ? getExpression().hashCode() : 0);
    result = 31 * result + (getUnit() != null ? getUnit().hashCode() : 0);
    result = 31 * result + (getCreatedAt() != null ? getCreatedAt().hashCode() : 0);
    return result;
  }

  public EntityVersionDao entity(EntityDao entity) {
    this.entity = entity;
    return this;
  }

  public EntityVersionDao version(Integer version) {
    this.version = version;
    return this;
  }

  public EntityVersionDao titles(List<LocalisableTextDao> titles) {
    this.titles = titles;
    return this;
  }

  public EntityVersionDao synonyms(List<LocalisableTextDao> synonyms) {
    this.synonyms = synonyms;
    return this;
  }

  public EntityVersionDao descriptions(List<LocalisableTextDao> descriptions) {
    this.descriptions = descriptions;
    return this;
  }

  public EntityVersionDao codes(List<CodeDao> codes) {
    this.codes = codes;
    return this;
  }

  public EntityVersionDao previousVersion(EntityVersionDao previousVersion) {
    this.previousVersion = previousVersion;
    return this;
  }

  public EntityVersionDao nextVersion(EntityVersionDao nextVersion) {
    this.nextVersion = nextVersion;
    return this;
  }

  public EntityVersionDao equivalentEntities(List<EntityVersionDao> equivalentEntities) {
    this.equivalentEntities = equivalentEntities;
    return this;
  }

  public EntityVersionDao author(String author) {
    this.author = author;
    return this;
  }

  public EntityVersionDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public DataType getDataType() {
    return dataType;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public RestrictionDao getRestriction() {
    return restriction;
  }

  public ExpressionDao getExpression() {
    return expression;
  }

  public Unit getUnit() {
    return unit;
  }

  public EntityDao getEntity() {
    return entity;
  }

  public Integer getVersion() {
    return version;
  }

  public List<LocalisableTextDao> getTitles() {
    return titles;
  }

  public List<LocalisableTextDao> getSynonyms() {
    return synonyms;
  }

  public List<LocalisableTextDao> getDescriptions() {
    return descriptions;
  }

  public List<CodeDao> getCodes() {
    return codes;
  }

  public EntityVersionDao getPreviousVersion() {
    return previousVersion;
  }

  public EntityVersionDao getNextVersion() {
    return nextVersion;
  }

  public List<EntityVersionDao> getEquivalentEntities() {
    return equivalentEntities;
  }

  public String getAuthor() {
    return author;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
