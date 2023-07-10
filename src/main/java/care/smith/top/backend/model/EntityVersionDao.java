package care.smith.top.backend.model;

import care.smith.top.model.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "entity_version")
@EntityListeners(AuditingEntityListener.class)
public class EntityVersionDao {
  @Id @GeneratedValue private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private EntityDao entity;

  @Column(nullable = false)
  private Integer version;

  @ElementCollection @OrderColumn private List<LocalisableTextDao> titles = null;

  @ElementCollection @OrderColumn private List<LocalisableTextDao> synonyms = null;

  @ElementCollection @OrderColumn private List<LocalisableTextDao> descriptions = null;

  @ElementCollection @OrderColumn private List<CodeDao> codes = null;

  @OneToOne private EntityVersionDao previousVersion;

  @OneToOne(mappedBy = "previousVersion")
  private EntityVersionDao nextVersion;

  @ManyToMany private Set<EntityVersionDao> equivalentEntityVersions = null;

  @ManyToMany(mappedBy = "equivalentEntityVersions")
  private Set<EntityVersionDao> equivalentEntityVersionOf = null;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @CreatedBy
  private UserDao author;

  private DataType dataType;

  private ItemType itemType;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private RestrictionDao restriction;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private ExpressionDao expression;

  private String unit;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  public void setAuthor(UserDao author) {
    this.author = author;
  }

  public EntityVersionDao() {}

  public EntityVersionDao(
      @NotNull EntityDao entity,
      @NotNull Integer version,
      List<LocalisableTextDao> titles,
      List<LocalisableTextDao> synonyms,
      List<LocalisableTextDao> descriptions,
      List<CodeDao> codes,
      DataType dataType,
      ItemType itemType,
      RestrictionDao restriction,
      ExpressionDao expression,
      String unit) {
    this.entity = entity;
    this.version = version;
    this.titles = titles;
    this.synonyms = synonyms;
    this.descriptions = descriptions;
    this.codes = codes;
    this.dataType = dataType;
    this.itemType = itemType;
    this.restriction = restriction;
    this.expression = expression;
    this.unit = unit;
  }

  public EntityVersionDao(@NotNull care.smith.top.model.Entity entity) {
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
      if (((Phenotype) entity).getRestriction() != null)
        restriction = new RestrictionDao(((Phenotype) entity).getRestriction());
      if (((Phenotype) entity).getExpression() != null)
        expression = new ExpressionDao(((Phenotype) entity).getExpression());
      unit = ((Phenotype) entity).getUnit();
    }
    if (entity instanceof CompositeConcept) {
      if (((CompositeConcept) entity).getExpression() != null){
        expression = new ExpressionDao(((CompositeConcept) entity).getExpression());
      }
    }
  }

  public EntityVersionDao dataType(DataType dataType) {
    this.dataType = dataType;
    return this;
  }

  public EntityVersionDao itemType(ItemType itemType) {
    this.itemType = itemType;
    return this;
  }

  public EntityVersionDao expression(ExpressionDao expression) {
    this.expression = expression;
    return this;
  }

  public EntityVersionDao addEquivalentEntityVersionsItem(
      EntityVersionDao equivalentEntityVersionsItem) {
    if (equivalentEntityVersions == null) equivalentEntityVersions = new HashSet<>();
    equivalentEntityVersions.add(equivalentEntityVersionsItem);
    return this;
  }

  public EntityVersionDao removeEquivalentEntityVersionsItem(
      EntityVersionDao equivalentEntityVersionsItem) {
    if (equivalentEntityVersions != null)
      equivalentEntityVersions.remove(equivalentEntityVersionsItem);
    return this;
  }

  public Long getId() {
    return id;
  }

  public EntityVersionDao id(@NotNull Long id) {
    this.id = id;
    return this;
  }

  public EntityVersionDao restriction(RestrictionDao restriction) {
    this.restriction = restriction;
    return this;
  }

  public care.smith.top.model.Entity toApiModel() {
    return entity.toApiModel(this);
  }

  public EntityVersionDao unit(String unit) {
    this.unit = unit;
    return this;
  }

  public EntityVersionDao entity(@NotNull EntityDao entity) {
    this.entity = entity;
    return this;
  }

  public EntityVersionDao version(@NotNull Integer version) {
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

  public EntityVersionDao equivalentEntityVersionOf(
      Set<EntityVersionDao> equivalentEntityVersionOf) {
    this.equivalentEntityVersionOf = equivalentEntityVersionOf;
    return this;
  }

  public EntityVersionDao equivalentEntities(Set<EntityVersionDao> equivalentEntities) {
    this.equivalentEntityVersions = equivalentEntities;
    return this;
  }

  public EntityVersionDao author(UserDao author) {
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

  public String getUnit() {
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

  public Set<EntityVersionDao> getEquivalentEntityVersionOf() {
    return equivalentEntityVersionOf;
  }

  public Set<EntityVersionDao> getEquivalentEntityVersions() {
    return equivalentEntityVersions;
  }

  public UserDao getAuthor() {
    return author;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
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

  @PreRemove
  private void preRemove() {
    equivalentEntityVersionOf.forEach(e -> e.getEquivalentEntityVersions().remove(this));
  }
}
