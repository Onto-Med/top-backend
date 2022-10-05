package care.smith.top.backend.model;

import care.smith.top.model.QueryResult;
import care.smith.top.model.QueryState;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class QueryResultDao {
  @Id @GeneratedValue private Long id;
  @ManyToOne private QueryDao query;
  private OffsetDateTime createdAt;
  private Long count;
  private OffsetDateTime finishedAt;
  private QueryState state;

  public QueryResultDao() {}

  public QueryResultDao(
      QueryDao query,
      OffsetDateTime createdAt,
      Long count,
      OffsetDateTime finishedAt,
      QueryState state) {
    this.query = query;
    this.createdAt = createdAt;
    this.count = count;
    this.finishedAt = finishedAt;
    this.state = state;
  }

  public Long getId() {
    return id;
  }

  public QueryResultDao id(Long id) {
    this.id = id;
    return this;
  }

  public QueryDao getQuery() {
    return query;
  }

  public QueryResultDao query(QueryDao query) {
    this.query = query;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public QueryResultDao createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getCount() {
    return count;
  }

  public QueryResultDao count(Long count) {
    this.count = count;
    return this;
  }

  public OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  public QueryResultDao finishedAt(OffsetDateTime finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  public QueryState getState() {
    return state;
  }

  public QueryResultDao state(QueryState state) {
    this.state = state;
    return this;
  }

  public QueryResult toApiModel() {
    return new QueryResult()
        .id(getQuery().getId())
        .createdAt(getCreatedAt())
        .count(getCount())
        .finishedAt(getFinishedAt())
        .state(getState());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryResultDao that = (QueryResultDao) o;
    return getId().equals(that.getId())
        && getQuery().equals(that.getQuery())
        && Objects.equals(getCreatedAt(), that.getCreatedAt())
        && Objects.equals(getCount(), that.getCount())
        && Objects.equals(getFinishedAt(), that.getFinishedAt())
        && getState() == that.getState();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(), getQuery(), getCreatedAt(), getCount(), getFinishedAt(), getState());
  }
}
