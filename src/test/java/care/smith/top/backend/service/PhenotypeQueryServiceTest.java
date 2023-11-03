package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import care.smith.top.backend.AbstractTest;
import care.smith.top.model.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class PhenotypeQueryServiceTest extends AbstractTest {
  static List<String> dataSources = Arrays.asList("Test_Data_Source_1", "Test_Data_Source_2");
  @Autowired PhenotypeQueryService queryService;
  @Autowired StorageProvider storageProvider;
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;

  @Test
  void executeQuery() {
    DataSource dataSource = new DataSource().id(dataSources.get(0)).queryType(QueryType.PHENOTYPE);
    Organisation orga = organisationService.createOrganisation(new Organisation().id("orga_1"));
    Repository repo1 =
        repositoryService.createRepository(
            orga.getId(),
            new Repository().id("repo_1").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Repository repo2 =
        repositoryService.createRepository(
            orga.getId(),
            new Repository().id("repo_2").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Phenotype phenotype1 =
        (Phenotype)
            entityService.createEntity(
                orga.getId(),
                repo1.getId(),
                new Phenotype()
                    .dataType(DataType.NUMBER)
                    .id("entity_1")
                    .entityType(EntityType.SINGLE_PHENOTYPE));

    PhenotypeQuery query =
        (PhenotypeQuery)
            new PhenotypeQuery()
                .addCriteriaItem(
                    (QueryCriterion)
                        new QueryCriterion()
                            .subjectId(phenotype1.getId())
                            .dateTimeRestriction(
                                (DateTimeRestriction)
                                    new DateTimeRestriction()
                                        .maxOperator(RestrictionOperator.LESS_THAN)
                                        .addValuesItem(null)
                                        .addValuesItem(
                                            LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                                        .type(DataType.DATE_TIME)))
                .type(QueryType.PHENOTYPE)
                .id(UUID.randomUUID())
                .dataSource(dataSource.getId());

    assertThatThrownBy(() -> queryService.enqueueQuery(orga.getId(), "invalid", query))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () -> queryService.enqueueQuery(orga.getId(), repo1.getId(), query),
            "data source was not added to organisation, enqueue should fail")
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_ACCEPTABLE);

    assertThatCode(() -> organisationService.addOrganisationDataSource(orga.getId(), dataSource))
        .doesNotThrowAnyException();

    assertThatCode(() -> queryService.enqueueQuery(orga.getId(), repo1.getId(), query))
        .doesNotThrowAnyException();

    await()
        .atMost(100, TimeUnit.SECONDS)
        .until(() -> storageProvider.getJobStats().getSucceeded() == 1);

    assertThat(queryService.getQueries(orga.getId(), repo1.getId(), null))
        .map(PhenotypeQuery.class::cast)
        .isNotNull()
        .anySatisfy(
            q -> {
              assertThat(q.getId()).isEqualTo(query.getId());
              assertThat(q.getDataSource()).isEqualTo(query.getDataSource());
              assertThat(q.getCriteria()).size().isEqualTo(1);
              assertThat(q.getCriteria().get(0))
                  .satisfies(
                      c -> {
                        assertThat(c.getSubjectId())
                            .isEqualTo(query.getCriteria().get(0).getSubjectId());
                        assertThat(c.getDateTimeRestriction())
                            .isEqualTo(query.getCriteria().get(0).getDateTimeRestriction());
                      });
            })
        .size()
        .isEqualTo(1);

    assertThatThrownBy(() -> queryService.getQueryById(orga.getId(), repo2.getId(), query.getId()))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(queryService.getQueryById(orga.getId(), repo1.getId(), query.getId()))
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(query.getId());
              assertThat(r.getResult().getCreatedAt()).isNotNull();
              assertThat(r.getResult().getFinishedAt()).isNotNull();
              assertThat(r.getResult().getCreatedAt().compareTo(r.getResult().getFinishedAt()))
                  .isLessThanOrEqualTo(0);
              assertThat(r.getResult().getCount()).isEqualTo(0);
              assertThat(r.getResult().getState()).isNotNull();
            });

    queryService.deleteQuery(orga.getId(), repo1.getId(), query.getId());
    assertThat(storageProvider.getJobStats().getSucceeded()).isEqualTo(0);
    assertThat(queryService.getQueries(orga.getId(), repo1.getId(), null)).isNullOrEmpty();
  }

  @Test
  void getDataAdapterConfig() {
    String id = dataSources.get(0);
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
            a -> assertThat(a.getId()).isEqualTo(dataSources.get(0)),
            a -> assertThat(a.getId()).isEqualTo(dataSources.get(1)));
  }

  @Test
  void getDataSources() {
    assertThat(queryService.getDataSources())
        .satisfiesExactly(
            d -> assertThat(d.getId()).isEqualTo(dataSources.get(0)),
            d -> assertThat(d.getId()).isEqualTo(dataSources.get(1)));
  }
}
