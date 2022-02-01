package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryRepositoryTest extends RepositoryTest {
  @Autowired private RepositoryRepository repositoryRepository;

  @Test
  void findByIdAndSuperDirectoryId() {
    Directory superDirectory = new Directory();
    Directory subDirectory = new Directory().addSuperDirectory(superDirectory);
    Repository repository = (Repository) new Repository().addSuperDirectory(subDirectory);

    repositoryRepository.save(repository);

    assertThat(
            repositoryRepository.findByIdAndSuperDirectoryId(
                repository.getId(), subDirectory.getId()))
        .hasValueSatisfying(r -> assertThat(r.getId()).isEqualTo(repository.getId()));
    assertThat(
            repositoryRepository.findByIdAndSuperDirectoryId(
                repository.getId(), superDirectory.getId()))
        .hasValueSatisfying(r -> assertThat(r.getId()).isEqualTo(repository.getId()));
  }

  @Test
  void findByNameContainingAndSuperDirectoryId() {
    Directory directory = new Directory();
    Repository repository1 =
        (Repository) new Repository().setName("Repository").addSuperDirectory(directory);
    Repository repository2 =
        (Repository) new Repository().setName("some name").addSuperDirectory(directory);

    repositoryRepository.saveAll(Arrays.asList(repository1, repository2));

    assertThat(
            repositoryRepository
                .findByNameContainingAndSuperDirectoryId(
                    "repo", directory.getId(), Pageable.ofSize(10))
                .getNumberOfElements())
        .isEqualTo(1);
  }
}
