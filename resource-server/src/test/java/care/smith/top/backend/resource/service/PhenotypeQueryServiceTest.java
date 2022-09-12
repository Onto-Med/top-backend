package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PhenotypeQueryServiceTest {
  @Autowired PhenotypeQueryService queryService;

  static List<DataSource> dataSources;

  @BeforeAll
  static void setup() {
    dataSources =
        Arrays.asList(
            new DataSource().id("Test_Data_Source_1").title("Test Data Source 1"),
            new DataSource().id("Test_Data_Source_2").title("Test Data Source 2"));
  }

  @Test
  void getDataAdapterConfig() {
    String id = dataSources.get(0).getId();
    assertThat(queryService.getDataAdapterConfig("invalid")).isNotPresent();
    assertThat(queryService.getDataAdapterConfig(id))
        .satisfies(
            a -> {
              assertThat(a).isPresent();
              assertThat(a.get().getId()).isEqualTo(id);
            });
  }

  @Test
  void getDataAdapterConfigs() {
    assertThat(queryService.getDataAdapterConfigs())
        .satisfiesExactly(
            a -> assertThat(a.getId()).isEqualTo(dataSources.get(0).getId()),
            a -> assertThat(a.getId()).isEqualTo(dataSources.get(1).getId()));
  }

  @Test
  void getDataSources() {
    assertThat(queryService.getDataSources()).isEqualTo(dataSources);
  }
}
