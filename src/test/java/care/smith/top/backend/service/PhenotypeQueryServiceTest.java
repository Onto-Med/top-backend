package care.smith.top.backend.service;

import care.smith.top.model.*;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class PhenotypeQueryServiceTest extends AbstractTest {
  static List<DataSource> dataSources;
  @Autowired PhenotypeQueryService queryService;
  @Autowired StorageProvider storageProvider;
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;

  @BeforeAll
  static void setup() {
    dataSources =
        Arrays.asList(
            new DataSource().id("Test_Data_Source_1").title("Test Data Source 1"),
            new DataSource().id("Test_Data_Source_2").title("Test Data Source 2"));
  }

  @Test
  void executeQuery() {
    Organisation orga = organisationService.createOrganisation(new Organisation().id("orga_1"));
    Repository repo1 =
        repositoryService.createRepository(orga.getId(), new Repository().id("repo_1"), null);
    Repository repo2 =
        repositoryService.createRepository(orga.getId(), new Repository().id("repo_2"), null);
    Phenotype phenotype1 =
        (Phenotype)
            entityService.createEntity(
                orga.getId(),
                repo1.getId(),
                new Entity().id("entity_1").entityType(EntityType.SINGLE_PHENOTYPE));

    Query query =
        new Query()
            .id(UUID.randomUUID())
            .addDataSourcesItem(new DataSource().id(dataSources.get(0).getId()))
            .addCriteriaItem(new QueryCriterion().subjectId(phenotype1.getId()));

    assertThatThrownBy(() -> queryService.enqueueQuery(orga.getId(), "invalid", query))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(queryService.enqueueQuery(orga.getId(), repo1.getId(), query)).isNotNull();
    await()
        .atMost(100, TimeUnit.SECONDS)
        .until(() -> storageProvider.getJobStats().getSucceeded() == 1);

    assertThat(queryService.getQueries(orga.getId(), repo1.getId(), null))
        .isNotNull()
        .size()
        .isEqualTo(1);

    assertThatThrownBy(
            () -> queryService.getQueryResult(orga.getId(), repo2.getId(), query.getId()))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(queryService.getQueryResult(orga.getId(), repo1.getId(), query.getId()))
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(query.getId());
              assertThat(r.getCreatedAt()).isNotNull();
              assertThat(r.getFinishedAt()).isNotNull();
              assertThat(r.getCreatedAt().compareTo(r.getFinishedAt())).isLessThanOrEqualTo(0);
              assertThat(r.getState()).isNotNull();
            });

    queryService.deleteQuery(orga.getId(), repo1.getId(), query.getId());
    assertThat(storageProvider.getJobStats().getSucceeded()).isEqualTo(0);
    assertThat(queryService.getQueries(orga.getId(), repo1.getId(), null)).isNullOrEmpty();
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
