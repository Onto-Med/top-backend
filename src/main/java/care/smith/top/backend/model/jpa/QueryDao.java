package care.smith.top.backend.model.jpa;

import care.smith.top.model.ConceptQuery;
import care.smith.top.model.PhenotypeQuery;
import care.smith.top.model.Query;
import care.smith.top.model.QueryType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity(name = "query")
@EntityListeners(AuditingEntityListener.class)
public class QueryDao {
  @Id private String id;
  private String name;
  private QueryType queryType;
  private String entityId;
  private String language;
  private String dataSource;
  @ElementCollection private List<QueryCriterionDao> criteria = null;
  @ElementCollection private List<ProjectionEntryDao> projection = null;

  @OneToOne(mappedBy = "query", cascade = CascadeType.ALL, orphanRemoval = true)
  private QueryResultDao result = null;

  @ManyToOne private RepositoryDao repository;

  @CreatedDate
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  public QueryDao() {}

  public QueryDao(
      @NotNull String id,
      String name,
      @NotNull String dataSource,
      List<QueryCriterionDao> criteria,
      List<ProjectionEntryDao> projection,
      RepositoryDao repository) {
    this.id = id;
    this.name = name;
    this.dataSource = dataSource;
    this.criteria = criteria;
    this.projection = projection;
    this.repository = repository;
    this.queryType = QueryType.PHENOTYPE;
  }

  public QueryDao(
      @NotNull String id,
      String name,
      String entityId,
      String language,
      @NotNull String dataSource,
      RepositoryDao repository) {
    this.id = id;
    this.name = name;
    this.entityId = entityId;
    this.dataSource = dataSource;
    this.repository = repository;
    this.queryType = QueryType.CONCEPT;
    this.language = language;
  }

  public QueryDao(@NotNull Query query) {
    this.id = query.getId().toString();
    this.name = query.getName();
    this.dataSource = query.getDataSource();

    if (query instanceof ConceptQuery) {
      this.queryType = QueryType.CONCEPT;
      this.entityId = ((ConceptQuery) query).getEntityId();
      this.language = ((ConceptQuery) query).getLanguage();
    } else if (query instanceof PhenotypeQuery) {
      this.queryType = QueryType.PHENOTYPE;
      if (((PhenotypeQuery) query).getCriteria() != null)
        this.criteria =
            ((PhenotypeQuery) query)
                .getCriteria().stream().map(QueryCriterionDao::new).collect(Collectors.toList());
      if (((PhenotypeQuery) query).getProjection() != null)
        this.projection =
            ((PhenotypeQuery) query)
                .getProjection().stream().map(ProjectionEntryDao::new).collect(Collectors.toList());
    }
  }

  public String getId() {
    return id;
  }

  public QueryDao id(@NotNull String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public QueryDao name(String name) {
    this.name = name;
    return this;
  }

  public QueryType getQueryType() {
    return queryType;
  }

  public String getDataSource() {
    return dataSource;
  }

  public QueryDao dataSource(String dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public QueryDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public List<QueryCriterionDao> getCriteria() {
    return criteria;
  }

  public QueryDao criteria(List<QueryCriterionDao> criteria) {
    this.criteria = criteria;
    return this;
  }

  public List<ProjectionEntryDao> getProjection() {
    return projection;
  }

  public QueryDao projection(List<ProjectionEntryDao> projection) {
    this.projection = projection;
    return this;
  }

  public String getEntityId() {
    return entityId;
  }

  public QueryDao entityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public QueryDao language(String language) {
    this.language = language;
    return this;
  }

  public RepositoryDao getRepository() {
    return repository;
  }

  public QueryDao repository(RepositoryDao repository) {
    this.repository = repository;
    return this;
  }

  public QueryResultDao getResult() {
    return result;
  }

  public QueryDao result(QueryResultDao result) {
    this.result = result;
    return this;
  }

  public Query toApiModel() {
    Query query = null;
    if (QueryType.PHENOTYPE.equals(queryType)) {
      query = new PhenotypeQuery();
      if (getCriteria() != null)
        ((PhenotypeQuery) query)
            .criteria(
                getCriteria().stream()
                    .map(QueryCriterionDao::toApiModel)
                    .collect(Collectors.toList()));
      if (getProjection() != null)
        ((PhenotypeQuery) query)
            .projection(
                getProjection().stream()
                    .map(ProjectionEntryDao::toApiModel)
                    .collect(Collectors.toList()));
    } else if (QueryType.CONCEPT.equals(queryType)) {
      query = new ConceptQuery().entityId(getEntityId()).language(getLanguage());
    }
    if (query == null) return null;
    return query
        .id(UUID.fromString(getId()))
        .name(getName())
        .type(queryType)
        .dataSource(getDataSource());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryDao queryDao = (QueryDao) o;
    if (this.queryType.equals(QueryType.PHENOTYPE)) {
      return getId().equals(queryDao.getId())
          && Objects.equals(getName(), queryDao.getName())
          && Objects.equals(getDataSource(), queryDao.getDataSource())
          && Objects.equals(getCriteria(), queryDao.getCriteria())
          && Objects.equals(getProjection(), queryDao.getProjection());
    } else if (this.queryType.equals(QueryType.CONCEPT)) {
      return getId().equals(queryDao.getId())
          && Objects.equals(getName(), queryDao.getName())
          && Objects.equals(getDataSource(), queryDao.getDataSource())
          && Objects.equals(getEntityId(), queryDao.getEntityId());
    } else return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getDataSource(), getCriteria(), getProjection());
  }
}
