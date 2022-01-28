package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassRelation;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassRepositoryTest extends RepositoryTest {
  @Autowired private ClassRepository classRepository;

  @Test
  public void findSubclasses() {
    UUID superclassUuid = UUID.randomUUID();
    Class cls = new Class(superclassUuid);
    Repository repository = new Repository();

    for (int i = 0; i < 10; i++) {
      ClassRelation relation = new ClassRelation().setSuperclass(cls).setRepository(repository);
      Class subclass = new Class(UUID.randomUUID()).addSuperClassRelation(relation);
      classRepository.save(subclass);
    }

    assertThat(classRepository.count()).isEqualTo(11);
    assertThat(classRepository.findSubclasses(cls, repository).count()).isEqualTo(10);
  }
}
