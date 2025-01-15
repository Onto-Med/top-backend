package care.smith.top.backend.model.jpa.datasource;

import care.smith.top.model.DataSource;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import care.smith.top.model.QueryType;
import org.apache.commons.lang3.StringUtils;

@Entity(name = "data_source")
public class DataSourceDao {
  @Id private String dataSourceId;
  private String title;

  public DataSourceDao() {}

  public DataSourceDao(@NotNull String dataSourceId) {
    this.dataSourceId = dataSourceId;
    this.title = StringUtils.capitalize(dataSourceId);
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
    return new DataSource().id(this.getDataSourceId()).title(this.getTitle()).queryType(QueryType.PHENOTYPE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataSourceDao that = (DataSourceDao) o;
    return Objects.equals(dataSourceId, that.dataSourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(dataSourceId);
  }
}
