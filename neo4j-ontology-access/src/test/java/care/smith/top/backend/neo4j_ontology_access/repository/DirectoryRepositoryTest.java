package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Directory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryRepositoryTest extends RepositoryTest {
  @Autowired DirectoryRepository directoryRepository;

  @Test
  void findByName() {
    String name1 = "Example Organisation";
    String name2 = "Other Organisation";

    directoryRepository.saveAll(
        Arrays.asList(
            new Directory().setName(name1),
            new Directory().setName(name1 + "1"),
            new Directory().setName(name2)));

    assertThat(directoryRepository.findAll(Pageable.ofSize(10)).getTotalElements()).isEqualTo(3);
    assertThat(
            directoryRepository.findByNameContaining(name1, Pageable.unpaged()).getTotalElements())
        .isEqualTo(2);
  }
}
