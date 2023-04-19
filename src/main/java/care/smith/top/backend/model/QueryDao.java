package care.smith.top.backend.model;

import care.smith.top.model.Query;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "query")
public class QueryDao {
  @Id private String id;
  private String name;
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
  }

  public QueryDao(@NotNull Query query) {
    this.id = query.getId().toString();
    this.name = query.getName();
    this.dataSources = query.getDataSources();
    if (query.getCriteria() != null)
      this.criteria =
          query.getCriteria().stream().map(QueryCriterionDao::new).collect(Collectors.toList());
    if (query.getProjection() != null)
      this.projection =
          query.getProjection().stream().map(ProjectionEntryDao::new).collect(Collectors.toList());
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
    Query query =
        new Query()
            .id(UUID.fromString(getId()))
            .name(getName())
            .dataSources(new ArrayList<>(getDataSources()));
    if (getCriteria() != null)
      query.criteria(
          getCriteria().stream().map(QueryCriterionDao::toApiModel).collect(Collectors.toList()));
    if (getProjection() != null)
      query.projection(
          getProjection().stream()
              .map(ProjectionEntryDao::toApiModel)
              .collect(Collectors.toList()));
    return query;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryDao queryDao = (QueryDao) o;
    return getId().equals(queryDao.getId())
        && Objects.equals(getName(), queryDao.getName())
        && Objects.equals(getDataSources(), queryDao.getDataSources())
        && Objects.equals(getCriteria(), queryDao.getCriteria())
        && Objects.equals(getProjection(), queryDao.getProjection());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getDataSources(), getCriteria(), getProjection());
  }
}
