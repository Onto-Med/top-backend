package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.neo4j_ontology_access.repository.DirectoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class OrganisationServiceTest extends Neo4jTest {
  @Autowired OrganisationService organisationService;
  @Autowired DirectoryRepository directoryRepository;

  @Test
  void createOrganisation() {
    /* Create super organisation */
    Organisation superOrganisation =
        new Organisation()
            .id("super_org")
            .name("Super organisation")
            .description("Example description");

    Organisation actual = organisationService.createOrganisation(superOrganisation);

    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(superOrganisation.getId());
    assertThat(actual.getName()).isEqualTo(superOrganisation.getName());
    assertThat(actual.getDescription()).isEqualTo(superOrganisation.getDescription());
    assertThat(actual.getCreatedAt()).isNotNull();
    assertThat(actual.getSuperOrganisation()).isNull();

    assertThatThrownBy(() -> organisationService.createOrganisation(superOrganisation))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    /* Create sub organisation */
    Organisation subOrganisation1 =
        new Organisation()
            .id("sub_org_1")
            .name("Sub organisation")
            .superOrganisation(superOrganisation);

    actual = organisationService.createOrganisation(subOrganisation1);

    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(subOrganisation1.getId());
    assertThat(actual.getName()).isEqualTo(subOrganisation1.getName());
    assertThat(actual.getDescription()).isNull();
    assertThat(actual.getCreatedAt()).isNotNull();
    assertThat(actual.getSuperOrganisation())
        .isNotNull()
        .hasFieldOrPropertyWithValue("id", superOrganisation.getId());

    /* Create another sub organisation */
    Organisation subOrganisation2 =
        new Organisation().id("sub_org_2").superOrganisation(superOrganisation);

    assertThatCode(() -> organisationService.createOrganisation(subOrganisation2))
        .doesNotThrowAnyException();

    /* Check DB consistence */
    Arrays.asList(subOrganisation1.getId(), subOrganisation2.getId())
        .forEach(
            id ->
                assertThat(directoryRepository.findById(id))
                    .isPresent()
                    .hasValueSatisfying(
                        d ->
                            assertThat(d.getSuperDirectories())
                                .isNotEmpty()
                                .anyMatch(sd -> sd.getId().equals(superOrganisation.getId()))
                                .size()
                                .isEqualTo(1)));
  }

  @Test
  void updateOrganisationById() {
    Organisation superOrganisation = new Organisation().id("super_org");
    Organisation actual = organisationService.createOrganisation(superOrganisation);

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isNull();
    assertThat(actual.getDescription()).isNull();

    assertThatThrownBy(
            () -> organisationService.updateOrganisationById("does not exist", superOrganisation))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    Organisation subOrganisation1 =
        new Organisation().id("sub_org_1").superOrganisation(superOrganisation);
    Organisation subOrganisation2 =
        new Organisation().id("sub_org_2").superOrganisation(superOrganisation);
    organisationService.createOrganisation(subOrganisation1);
    organisationService.createOrganisation(subOrganisation2);

    superOrganisation.name("Super organisation").description("Some description");
    assertThat(
            organisationService.updateOrganisationById(
                superOrganisation.getId(), superOrganisation))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a).isNotNull();
              assertThat(a.getId()).isEqualTo(superOrganisation.getId());
              assertThat(a.getName()).isNotNull().isEqualTo(superOrganisation.getName());
              assertThat(a.getDescription())
                  .isNotNull()
                  .isEqualTo(superOrganisation.getDescription());
              assertThat(a.getCreatedAt()).isEqualTo(actual.getCreatedAt());
            });

    subOrganisation1.name("Sub organisation 1");
    assertThat(
            organisationService.updateOrganisationById(subOrganisation1.getId(), subOrganisation1))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a).isNotNull();
              assertThat(a.getName()).isEqualTo(subOrganisation1.getName());
              assertThat(a.getSuperOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", superOrganisation.getId());
            });

    assertThat(directoryRepository.findById(subOrganisation2.getId()))
        .isPresent()
        .hasValueSatisfying(
            d -> {
              assertThat(d.getName()).isNull();
              assertThat(d.getSuperDirectories())
                  .isNotEmpty()
                  .anyMatch(sd -> sd.getId().equals(superOrganisation.getId()));
            });
  }

  @Test
  void deleteOrganisationByName() {}

  @Test
  void getOrganisation() {}

  @Test
  void getOrganisations() {}
}
