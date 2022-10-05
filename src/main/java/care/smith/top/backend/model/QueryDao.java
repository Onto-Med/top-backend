package care.smith.top.backend.model;

import care.smith.top.model.DataSource;
import care.smith.top.model.Query;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity(name = "query")
public class QueryDao {
  @Id private UUID id;
  private String name;
  @ElementCollection private List<DataSource> dataSources = null;
  @ElementCollection private List<QueryCriterionDao> criteria = null;
  @ElementCollection private List<ProjectionEntryDao> projection = null;

  @OneToMany(cascade = CascadeType.REMOVE)
  private Set<QueryResultDao> results = null;

  @ManyToOne private RepositoryDao repository;

  public QueryDao() {}

  public QueryDao(
      UUID id,
      String name,
      List<DataSource> dataSources,
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

  public QueryDao(Query query) {
    this.id = query.getId();
    this.name = query.getName();
    this.dataSources = query.getDataSources();
    if (query.getCriteria() != null)
      this.criteria =
          query.getCriteria().stream().map(QueryCriterionDao::new).collect(Collectors.toList());
    if (query.getProjection() != null)
      this.projection =
          query.getProjection().stream().map(ProjectionEntryDao::new).collect(Collectors.toList());
  }

  public UUID getId() {
    return id;
  }

  public QueryDao id(UUID id) {
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

  public List<DataSource> getDataSources() {
    return dataSources;
  }

  public QueryDao dataSources(List<DataSource> dataSources) {
    this.dataSources = dataSources;
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

  public Set<QueryResultDao> getResults() {
    return results;
  }

  public QueryDao results(Set<QueryResultDao> results) {
    this.results = results;
    return this;
  }

  public Query toApiModel() {
    Query query = new Query().id(getId()).name(getName()).dataSources(getDataSources());
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
