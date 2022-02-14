package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryRepositoryTest extends RepositoryTest {
  @Autowired DirectoryRepository directoryRepository;

  @Test
  void findByNameContaining() {
    String name1 = "Example Organisation";
    String name2 = "Other Organisation";

    directoryRepository.saveAll(
        Arrays.asList(
            new Directory().setName(name1).setDescription("Description"),
            new Directory().setName(name1 + "1"),
            new Directory().setName(name2)));

    assertThat(directoryRepository.findAll(Pageable.ofSize(10))).size().isEqualTo(3);
    assertThat(directoryRepository.findByNameContainingIgnoreCase(name1)).size().isEqualTo(2);
    assertThat(directoryRepository.findByDescriptionContainingIgnoreCase("desc")).size().isEqualTo(1);
  }

  @Test
  void findByType() {
    directoryRepository.saveAll(
      Arrays.asList(
        new Directory().setTypes(Collections.singleton("Organisation")),
        new Directory().setTypes(Collections.singleton("Project"))
      )
    );

    assertThat(directoryRepository.findByType("Organisation")).size().isEqualTo(1);
  }
}
