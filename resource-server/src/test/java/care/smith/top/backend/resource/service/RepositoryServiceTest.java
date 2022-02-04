package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.model.Repository;
import care.smith.top.backend.neo4j_ontology_access.repository.DirectoryRepository;
import care.smith.top.backend.neo4j_ontology_access.repository.RepositoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

class RepositoryServiceTest extends Neo4jTest {
  @Autowired RepositoryService repositoryService;
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired DirectoryRepository directoryRepository;

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

    assertThat(repositoryService.createRepository(organisation.getId(), repository, null))
        .isNotNull()
        .satisfies(
            r -> {
              assertThat(r.getId()).isEqualTo(repository.getId());
              assertThat(r.getName()).isEqualTo(repository.getName());
              assertThat(r.getDescription()).isEqualTo(repository.getDescription());
              assertThat(r.getCreatedAt()).isNotNull();
              assertThat(r.getOrganisation())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", organisation.getId());
            });

    assertThat(
            repositoryRepository.findByIdAndSuperDirectoryId(
                repository.getId(), organisation.getId()))
        .isPresent();

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
        .allMatch(
            r ->
                r.getSuperDirectories().stream()
                    .allMatch(d -> d.getId().equals(organisation.getId())))
        .size()
        .isEqualTo(2);
  }

  @Test
  void deleteRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository1 =
        new Repository().id("repo_1").name("Repository 1").description("Some description");
    Repository repository2 =
        new Repository().id("repo_2").name("Repository 2").description("Some description");

    assertThat(repositoryService.createRepository(organisation.getId(), repository1, null))
        .isNotNull();

    assertThat(repositoryService.createRepository(organisation.getId(), repository2, null))
        .isNotNull();

    assertThatCode(
            () ->
                repositoryService.deleteRepository(repository1.getId(), organisation.getId(), null))
        .doesNotThrowAnyException();

    assertThat(
            repositoryRepository.findByIdAndSuperDirectoryId(
                repository1.getId(), organisation.getId()))
        .isNotPresent();

    assertThat(directoryRepository.findById(organisation.getId())).isPresent();

    assertThat(
            repositoryRepository.findByIdAndSuperDirectoryId(
                repository2.getId(), organisation.getId()))
        .isPresent()
        .hasValueSatisfying(
            r ->
                assertThat(r.getSuperDirectories())
                    .isNotEmpty()
                    .allMatch(d -> d.getId().equals(organisation.getId())));
  }

  @Test
  void getRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        new Repository().id("repo").name("Repository").description("Some description");

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
  void updateRepository() {}

  @Test
  void getRepositories() {}

  @Test
  void getRepositoriesByOrganisationId() {}
}
