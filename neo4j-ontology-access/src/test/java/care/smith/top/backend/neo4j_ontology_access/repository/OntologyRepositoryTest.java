package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Ontology;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class OntologyRepositoryTest extends RepositoryTest {
  @Autowired OntologyRepository ontologyRepository;

  @Test
  void findByIdAndRepositoryId() {
    Repository repository = new Repository();
    Ontology ontology = new Ontology().setRepository(repository);
    ontologyRepository.save(ontology);

    assertThat(ontologyRepository.findByIdAndRepositoryId(ontology.getId(), repository.getId()))
        .hasValueSatisfying(o -> assertThat(o.getId()).isEqualTo(ontology.getId()));
  }
}
