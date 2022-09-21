package care.smith.top.backend.service;

import care.smith.top.backend.model.Organisation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class OrganisationServiceTest extends AbstractTest {

  @Test
  void createOrganisation() {
    /* Create super organisation */
    Organisation superOrganisation =
        new Organisation()
            .id("super_org/")
            .name("Super organisation")
            .description("Example description");

    assertThatThrownBy(() -> organisationApiDelegate.createOrganisation(superOrganisation, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

    superOrganisation.id("super_org");

    assertThat(organisationService.createOrganisation(superOrganisation))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a).isNotNull();
              assertThat(a.getId()).isEqualTo(superOrganisation.getId());
              assertThat(a.getName()).isEqualTo(superOrganisation.getName());
              assertThat(a.getDescription()).isEqualTo(superOrganisation.getDescription());
              assertThat(a.getCreatedAt()).isNotNull();
              assertThat(a.getSuperOrganisation()).isNull();
            });

    assertThatThrownBy(() -> organisationService.createOrganisation(superOrganisation))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    /* Create sub organisation */
    Organisation subOrganisation1 =
        new Organisation()
            .id("sub_org_1")
            .name("Sub organisation")
            .superOrganisation(superOrganisation);

    assertThat(organisationService.createOrganisation(subOrganisation1))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a).isNotNull();
              assertThat(a.getId()).isEqualTo(subOrganisation1.getId());
              assertThat(a.getName()).isEqualTo(subOrganisation1.getName());
              assertThat(a.getDescription()).isNull();
              assertThat(a.getCreatedAt()).isNotNull();
              assertThat(a.getSuperOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", superOrganisation.getId());
            });

    /* Create another sub organisation */
    Organisation subOrganisation2 =
        new Organisation().id("sub_org_2").superOrganisation(superOrganisation);

    assertThatCode(() -> organisationService.createOrganisation(subOrganisation2))
        .doesNotThrowAnyException();

    /* Check DB consistence */
    assertThat(organisationService.getOrganisation(superOrganisation.getId(), null))
        .isNotNull()
        .satisfies(
            o -> {
              assertThat(o.getName()).isEqualTo(superOrganisation.getName());
              assertThat(o.getDescription()).isEqualTo(superOrganisation.getDescription());
              assertThat(o.getCreatedAt()).isNotNull();
              assertThat(o.getUpdatedAt()).isNotNull();
            });

    Arrays.asList(subOrganisation1.getId(), subOrganisation2.getId())
        .forEach(
            id ->
                assertThat(organisationRepository.findById(id))
                    .isPresent()
                    .hasValueSatisfying(
                        o ->
                            assertThat(o.getSuperOrganisation().getId())
                                .isEqualTo(superOrganisation.getId())));
  }

  @Test
  void updateOrganisationById() {
    Organisation superOrganisation = new Organisation().id("super_org");
    Organisation actual = organisationService.createOrganisation(superOrganisation);

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isNull();
    assertThat(actual.getDescription()).isNull();
    assertThat(actual.getCreatedAt()).isNotNull();
    assertThat(actual.getUpdatedAt()).isNotNull();

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

    assertThat(organisationService.updateOrganisationById(
      superOrganisation.getId(), superOrganisation))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a).isNotNull();
              assertThat(a.getId()).isEqualTo(superOrganisation.getId());
              assertThat(a.getName()).isNotNull().isEqualTo(superOrganisation.getName());
              assertThat(a.getDescription()).isEqualTo(superOrganisation.getDescription());
              assertThat(a.getCreatedAt()).isNotNull();
              assertThat(a.getUpdatedAt()).isAfter(actual.getUpdatedAt());
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
              assertThat(a.getCreatedAt()).isNotNull();
              assertThat(a.getUpdatedAt()).isAfter(actual.getUpdatedAt());
            });

    assertThat(organisationRepository.findById(subOrganisation2.getId()))
        .isPresent()
        .hasValueSatisfying(
            o -> {
              assertThat(o.getName()).isNull();
              assertThat(o.getSuperOrganisation().getId()).isEqualTo(superOrganisation.getId());
              assertThat(o.getCreatedAt()).isNotNull();
            });
  }

  @Test
  void deleteOrganisationById() {
    Organisation superOrganisation = new Organisation().id("super_org");
    organisationService.createOrganisation(superOrganisation);

    Organisation subOrganisation =
        new Organisation().id("sub_org").superOrganisation(superOrganisation);
    organisationService.createOrganisation(subOrganisation);

    assertThatCode(() -> organisationService.deleteOrganisationById(superOrganisation.getId()))
        .doesNotThrowAnyException();

    assertThat(organisationRepository.findById(superOrganisation.getId())).isEmpty();
    assertThat(organisationRepository.findById(subOrganisation.getId()))
        .isPresent()
        .hasValueSatisfying(o -> assertThat(o.getSuperOrganisation()).isNull());
  }

  @Test
  void getOrganisation() {
    Organisation superOrganisation = new Organisation().id("super_org").name("Super organisation");
    organisationService.createOrganisation(superOrganisation);

    Organisation subOrganisation =
        new Organisation()
            .id("sub_org")
            .name("Sub organisation")
            .superOrganisation(superOrganisation);
    organisationService.createOrganisation(subOrganisation);

    assertThat(organisationService.getOrganisation(subOrganisation.getId(), null))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a.getId()).isEqualTo(subOrganisation.getId());
              assertThat(a.getName()).isEqualTo(subOrganisation.getName());
              assertThat(a.getDescription()).isNull();
              assertThat(a.getCreatedAt()).isNotNull();
              assertThat(a.getSuperOrganisation())
                  .isNotNull()
                  .satisfies(
                      o -> {
                        assertThat(o.getId()).isEqualTo(superOrganisation.getId());
                        assertThat(o.getName()).isEqualTo(superOrganisation.getName());
                        assertThat(o.getDescription()).isNull();
                        assertThat(o.getCreatedAt()).isNotNull();
                        assertThat(o.getSuperOrganisation()).isNull();
                      });
            });
  }

  @Test
  void getOrganisations() {
    organisationService.createOrganisation(new Organisation().id("org_1").name("Organisation"));
    organisationService.createOrganisation(
        new Organisation().id("org_2").name("Other organisation"));

    assertThat(organisationService.getOrganisations("Organ", 1, null))
        .isNotEmpty()
        .anyMatch(o -> o.getId().equals("org_1"))
        .anyMatch(o -> o.getId().equals("org_2"))
        .size()
        .isEqualTo(2);

    assertThat(organisationService.getOrganisations("other", 1, null))
        .isNotEmpty()
        .anyMatch(o -> o.getId().equals("org_2"))
        .size()
        .isEqualTo(1);

    assertThat(organisationService.getOrganisations("not matching string", 1, null)).isEmpty();

    assertThat(organisationService.getOrganisations(null, 1, null)).size().isEqualTo(2);
  }
}
