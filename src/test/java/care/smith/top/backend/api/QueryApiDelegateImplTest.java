package care.smith.top.backend.api;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.AbstractTest;
import care.smith.top.model.DataSource;
import care.smith.top.model.Organisation;
import care.smith.top.model.QueryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class QueryApiDelegateImplTest extends AbstractTest {
  @Autowired private QueryApiDelegateImpl queryApi;
  @Autowired private OrganisationApiDelegateImpl organisationApi;

  @Test
  void getDataSources() {
    assertThat(queryApi.getDataSources(null).getBody()).isNotNull().size().isEqualTo(3);
    assertThat(queryApi.getDataSources(QueryType.PHENOTYPE).getBody())
        .isNotNull()
        .size()
        .isEqualTo(2);
  }

  @Test
  void getOrganisationDataSources() {
    Organisation organisation = new Organisation().id("orga");
    DataSource dataSource = new DataSource().id("Test_Data_Source_1");

    assertThatThrownBy(
            () -> queryApi.getOrganisationDataSources("does not exist", null).getStatusCode())
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(organisationApi.createOrganisation(organisation, null).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);

    assertThat(queryApi.getOrganisationDataSources(organisation.getId(), null).getBody())
        .isNotNull()
        .isEmpty();

    assertThatThrownBy(() -> queryApi.addOrganisationDataSource("does not exist", dataSource))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(() -> queryApi.removeOrganisationDataSource("does not exist", dataSource))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () -> queryApi.removeOrganisationDataSource(organisation.getId(), dataSource))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(queryApi.addOrganisationDataSource(organisation.getId(), dataSource).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);

    assertThat(queryApi.getOrganisationDataSources(organisation.getId(), null).getBody())
        .isNotNull()
        .allSatisfy(ds -> assertThat(ds.getId()).isEqualTo(dataSource.getId()));

    assertThat(
            queryApi
                .getOrganisationDataSources(organisation.getId(), QueryType.PHENOTYPE)
                .getBody())
        .isNotNull()
        .allSatisfy(ds -> assertThat(ds.getId()).isEqualTo(dataSource.getId()));
    assertThat(
            queryApi.getOrganisationDataSources(organisation.getId(), QueryType.CONCEPT).getBody())
        .isNotNull()
        .isEmpty();

    assertThat(
            queryApi.removeOrganisationDataSource(organisation.getId(), dataSource).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);

    assertThat(queryApi.getOrganisationDataSources(organisation.getId(), null).getBody())
        .isNotNull()
        .isEmpty();
  }
}
