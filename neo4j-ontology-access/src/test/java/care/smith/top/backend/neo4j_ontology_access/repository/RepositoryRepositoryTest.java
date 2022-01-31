package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryRepositoryTest extends RepositoryTest {
  @Autowired private RepositoryRepository repositoryRepository;

  @Test
  void findByIdAndSuperDirectoryId() {
    Directory superDirectory = new Directory();
    Directory subDirectory =
        new Directory().setSuperDirectories(Collections.singleton(superDirectory));
    Repository repository =
        (Repository) new Repository().setSuperDirectories(Collections.singleton(subDirectory));

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
}
