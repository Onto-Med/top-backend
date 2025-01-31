package care.smith.top.backend.model.jpa.datasource;

import care.smith.top.model.DataSource;
import care.smith.top.model.QueryType;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

@Entity(name = "data_source")
public class DataSourceDao {
  @Id private String dataSourceId;
  private String title;

  public DataSourceDao() {}

  public DataSourceDao(@NotNull String dataSourceId) {
    this(dataSourceId, StringUtils.capitalize(dataSourceId));
  }

  public DataSourceDao(@NotNull String dataSourceId, String title) {
    this.dataSourceId = dataSourceId;
    this.title = title;
  }

  public String getDataSourceId() {
    return dataSourceId;
  }

  public String getTitle() {
    return title;
  }

  public DataSourceDao title(String title) {
    this.title = title;
    return this;
  }

  public DataSource toApiModel() {
    return new DataSource()
        .id(this.getDataSourceId())
        .title(this.getTitle())
        .queryType(QueryType.PHENOTYPE)
        .local(true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataSourceDao that = (DataSourceDao) o;
    return Objects.equals(getDataSourceId(), that.getDataSourceId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(dataSourceId);
  }
}
