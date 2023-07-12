package care.smith.top.backend.model;

import care.smith.top.model.ConceptQuery;
import care.smith.top.model.PhenotypeQuery;
import care.smith.top.model.Query;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
  private Query.TypeEnum queryType;
  private String entityId;
  @ElementCollection private List<String> dataSources = null;
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
      List<String> dataSources,
      List<QueryCriterionDao> criteria,
      List<ProjectionEntryDao> projection,
      RepositoryDao repository) {
    this.id = id;
    this.name = name;
    this.dataSources = dataSources;
    this.criteria = criteria;
    this.projection = projection;
    this.repository = repository;
    this.queryType = Query.TypeEnum.PHENOTYPE;
  }

  public QueryDao(
      @NotNull String id,
      String name,
      String entityId,
      List<String> dataSources,
      RepositoryDao repository) {
    this.id = id;
    this.name = name;
    this.entityId = entityId;
    this.dataSources = dataSources;
    this.repository = repository;
    this.queryType = Query.TypeEnum.CONCEPT;
  }

  public QueryDao(@NotNull PhenotypeQuery query) {
    this.id = query.getId().toString();
    this.name = query.getName();
    this.dataSources = query.getDataSources();
    this.queryType = Query.TypeEnum.PHENOTYPE;
    if (query.getCriteria() != null)
      this.criteria =
          query.getCriteria().stream().map(QueryCriterionDao::new).collect(Collectors.toList());
    if (query.getProjection() != null)
      this.projection =
          query.getProjection().stream().map(ProjectionEntryDao::new).collect(Collectors.toList());
  }

  public QueryDao(@NotNull ConceptQuery query) {
    this.id = query.getId().toString();
    this.name = query.getName();
    this.entityId = query.getEntityId();
    this.dataSources = query.getDataSources();
    this.queryType = Query.TypeEnum.CONCEPT;
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

  public List<String> getDataSources() {
    return dataSources;
  }

  public QueryDao dataSources(List<String> dataSources) {
    this.dataSources = dataSources;
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
    if (this.queryType.equals(Query.TypeEnum.PHENOTYPE)) {
      query =
          new PhenotypeQuery()
              .id(UUID.fromString(getId()))
              .name(getName())
              .dataSources(new ArrayList<>(getDataSources()));
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
    } else if (this.queryType.equals(Query.TypeEnum.CONCEPT)) {
      query =
          new ConceptQuery()
              .id(UUID.fromString(getId()))
              .name(getName())
              .dataSources(new ArrayList<>(getDataSources()));
      ((ConceptQuery) query).entityId(getEntityId());
    }
    return query;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryDao queryDao = (QueryDao) o;
    if (this.queryType.equals(Query.TypeEnum.PHENOTYPE)) {
      return getId().equals(queryDao.getId())
          && Objects.equals(getName(), queryDao.getName())
          && Objects.equals(getDataSources(), queryDao.getDataSources())
          && Objects.equals(getCriteria(), queryDao.getCriteria())
          && Objects.equals(getProjection(), queryDao.getProjection());
    } else if (this.queryType.equals(Query.TypeEnum.CONCEPT)) {
      return getId().equals(queryDao.getId())
          && Objects.equals(getName(), queryDao.getName())
          && Objects.equals(getDataSources(), queryDao.getDataSources())
          && Objects.equals(getEntityId(), queryDao.getEntityId());
    } else return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getDataSources(), getCriteria(), getProjection());
  }
}
