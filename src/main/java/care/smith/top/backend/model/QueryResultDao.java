package care.smith.top.backend.model;

import care.smith.top.model.QueryResult;
import care.smith.top.model.QueryState;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "query_result")
public class QueryResultDao {
  @Id @GeneratedValue private Long id;

  @OneToOne(optional = false)
  private QueryDao query;

  private OffsetDateTime createdAt;
  private Long count;
  private OffsetDateTime finishedAt;

  @Column(length = 5000)
  private String message;

  private QueryState state;

  public QueryResultDao() {}

  public QueryResultDao(
      @NotNull QueryDao query,
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

  public QueryResultDao id(@NotNull Long id) {
    this.id = id;
    return this;
  }

  public QueryDao getQuery() {
    return query;
  }

  public QueryResultDao query(@NotNull QueryDao query) {
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

  public String getMessage() {
    return message;
  }

  public QueryResultDao message(String message) {
    this.message = message;
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
        .id(UUID.fromString(getQuery().getId()))
        .createdAt(getCreatedAt())
        .count(getCount())
        .finishedAt(getFinishedAt())
        .message(getMessage())
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
        && Objects.equals(getMessage(), that.getMessage())
        && getState() == that.getState();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(), getQuery(), getCreatedAt(), getCount(), getFinishedAt(), getMessage(), getState());
  }
}
