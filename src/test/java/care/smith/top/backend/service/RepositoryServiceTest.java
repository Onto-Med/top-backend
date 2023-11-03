package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.model.jpa.RepositoryDao;
import care.smith.top.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RepositoryServiceTest extends AbstractTest {
  @Test
  void createRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        new Repository().id("repo").name("Repository").description("Some description");

    assertThatThrownBy(
            () -> repositoryService.createRepository("does not exists", repository, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () -> repositoryService.createRepository(organisation.getId(), repository, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_ACCEPTABLE);

    repository.repositoryType(RepositoryType.PHENOTYPE_REPOSITORY);

    assertThat(repositoryService.createRepository(organisation.getId(), repository, null))
        .isNotNull()
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(repository.getId());
              assertThat(r.getName()).isEqualTo(repository.getName());
              assertThat(r.getDescription()).isEqualTo(repository.getDescription());
              assertThat(r.isPrimary()).isFalse();
              assertThat(r.getRepositoryType()).isEqualTo(repository.getRepositoryType());
              assertThat(r.getCreatedAt()).isNotNull();
              assertThat(r.getUpdatedAt()).isNotNull();
              assertThat(r.getOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", organisation.getId());
            });

    assertThat(repositoryRepository.findById(repository.getId()))
        .isPresent()
        .hasValueSatisfying(
            r -> assertThat(r.getOrganisation().getId()).isEqualTo(organisation.getId()));

    assertThatThrownBy(
            () -> repositoryService.createRepository(organisation.getId(), repository, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    assertThat(
            repositoryService.createRepository(organisation.getId(), repository.id("repo_2"), null))
        .isNotNull()
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(repository.getId());
              assertThat(r.getOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", organisation.getId());
            });

    assertThat(repositoryRepository.findAll())
        .map(RepositoryDao::getOrganisation)
        .allMatch(o -> o.getId().equals(organisation.getId()))
        .size()
        .isEqualTo(2);
  }

  @Test
  void deleteRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository1 =
        new Repository()
            .id("repo_1")
            .name("Repository 1")
            .description("Some description")
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY);
    Repository repository2 =
        new Repository()
            .id("repo_2")
            .name("Repository 2")
            .description("Some description")
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY);

    assertThat(repositoryService.createRepository(organisation.getId(), repository1, null))
        .isNotNull();

    assertThat(repositoryService.createRepository(organisation.getId(), repository2, null))
        .isNotNull();

    assertThatCode(
            () ->
                repositoryService.deleteRepository(repository1.getId(), organisation.getId(), null))
        .doesNotThrowAnyException();

    assertThat(repositoryRepository.findById(repository1.getId())).isNotPresent();

    assertThat(organisationRepository.findById(organisation.getId())).isPresent();

    assertThat(repositoryRepository.findById(repository2.getId()))
        .isPresent()
        .hasValueSatisfying(
            r -> assertThat(r.getOrganisation().getId()).isEqualTo(organisation.getId()));

    assertThatCode(
            () ->
                entityService.createEntity(
                    organisation.getId(),
                    repository2.getId(),
                    new Entity().id("cat").entityType(EntityType.CATEGORY)))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                repositoryService.deleteRepository(repository2.getId(), organisation.getId(), null))
        .doesNotThrowAnyException();

    assertThat(repositoryRepository.count()).isEqualTo(0);
    assertThat(entityRepository.count()).isEqualTo(0);
  }

  @Test
  void getRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        new Repository()
            .id("repo")
            .name("Repository")
            .description("Some description")
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY);

    assertThat(repositoryService.createRepository(organisation.getId(), repository, null))
        .isNotNull();

    assertThat(repositoryService.getRepository(organisation.getId(), repository.getId(), null))
        .isNotNull()
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(repository.getId());
              assertThat(r.getName()).isEqualTo(repository.getName());
              assertThat(r.getDescription()).isEqualTo(repository.getDescription());
              assertThat(r.getCreatedAt()).isNotNull();
              assertThat(r.getUpdatedAt()).isNotNull();
              assertThat(r.getOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", organisation.getId());
            });

    assertThatThrownBy(
            () -> repositoryService.getRepository(organisation.getId(), "does not exist", null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () -> repositoryService.getRepository("does not exist", repository.getId(), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void updateRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    assertThat(repository).isNotNull();

    Repository expected =
        new Repository().id(repository.getId()).name("Repository").description("Some description");
    assertThat(
            repositoryService.updateRepository(
                organisation.getId(), repository.getId(), expected, null))
        .isNotNull()
        .satisfies(
            a -> {
              assertThat(a.getId()).isEqualTo(repository.getId());
              assertThat(a.getName()).isEqualTo(expected.getName());
              assertThat(a.getDescription()).isEqualTo(expected.getDescription());
              assertThat(a.getCreatedAt()).isNotNull();
              assertThat(a.getUpdatedAt()).isAfter(repository.getUpdatedAt());
              assertThat(a.getOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", repository.getOrganisation().getId());
            });

    assertThatThrownBy(
            () ->
                repositoryService.updateRepository(
                    organisation.getId(), "does not exist", expected, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () ->
                repositoryService.updateRepository(
                    "does not exist", repository.getId(), expected, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void getRepositories() {
    Organisation organisation1 =
        organisationService.createOrganisation(new Organisation().id("org_1"));
    Organisation organisation2 =
        organisationService.createOrganisation(new Organisation().id("org_2"));
    repositoryService.createRepository(
        organisation1.getId(),
        new Repository()
            .id("repo_1")
            .name("Repository")
            .primary(true)
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
        null);
    repositoryService.createRepository(
        organisation2.getId(),
        new Repository()
            .id("repo_2")
            .name("Another repository")
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
        null);

    assertThat(repositoryService.getRepositories(null, "another", null, null, 1))
        .isNotEmpty()
        .size()
        .isEqualTo(1);
    assertThat(repositoryService.getRepositories(null, "repo", null, null, 1))
        .isNotEmpty()
        .size()
        .isEqualTo(2);
    assertThat(repositoryService.getRepositories(null, "something else", null, null, 1)).isEmpty();
    assertThat(repositoryService.getRepositories(null, null, true, null, 1))
        .isNotEmpty()
        .size()
        .isEqualTo(1);
    assertThat(repositoryService.getRepositories(null, null, false, null, 1))
        .isNotEmpty()
        .size()
        .isEqualTo(1);
    assertThat(repositoryService.getRepositories(null, null, null, null, 1))
        .isNotEmpty()
        .size()
        .isEqualTo(2);
  }

  @Test
  void getRepositoriesByOrganisationId() {
    Organisation organisation1 =
        organisationService.createOrganisation(new Organisation().id("org_1"));
    Organisation organisation2 =
        organisationService.createOrganisation(new Organisation().id("org_2"));
    Repository repository1 =
        repositoryService.createRepository(
            organisation1.getId(),
            new Repository()
                .id("repo_1")
                .name("Repository")
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    assertThat(
            repositoryService.createRepository(
                organisation2.getId(),
                new Repository()
                    .id("repo_2")
                    .name("Another repository")
                    .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
                null))
        .isNotNull();

    assertThat(
            repositoryService.getRepositoriesByOrganisationId(
                organisation1.getId(), null, "another", null, 1))
        .isNullOrEmpty();

    assertThat(
            repositoryService.getRepositoriesByOrganisationId(
                organisation1.getId(), null, "repo", null, 1))
        .isNotEmpty()
        .allMatch(r -> r.getId().equals(repository1.getId()))
        .size()
        .isEqualTo(1);

    assertThat(
            repositoryService.getRepositoriesByOrganisationId(
                organisation1.getId(), null, null, null, 1))
        .isNotEmpty()
        .allMatch(r -> r.getId().equals(repository1.getId()))
        .size()
        .isEqualTo(1);

    assertThat(
            repositoryService.getRepositoriesByOrganisationId(
                "something else", null, "repo", null, 1))
        .isNullOrEmpty();

    assertThat(
            repositoryService.getRepositoriesByOrganisationId(
                organisation1.getId(), null, null, RepositoryType.PHENOTYPE_REPOSITORY, 1))
        .isNotEmpty();

    assertThat(
            repositoryService.getRepositoriesByOrganisationId(
                organisation1.getId(), null, null, RepositoryType.CONCEPT_REPOSITORY, 1))
        .isNullOrEmpty();
  }
}
