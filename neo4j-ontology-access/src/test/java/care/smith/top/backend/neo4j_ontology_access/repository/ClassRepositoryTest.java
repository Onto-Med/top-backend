package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassRelation;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassRepositoryTest extends RepositoryTest {
  @Autowired private ClassRepository classRepository;
  @Autowired private RepositoryRepository repositoryRepository;

  @Test
  public void findSubclasses() {
    UUID superclassUuid = UUID.randomUUID();
    Class cls = new Class(superclassUuid);
    Repository repository = new Repository();

    for (int i = 0; i < 10; i++) {
      ClassRelation relation =
          new ClassRelation().setSuperclass(cls).setOwnerId(repository.getId());
      Class subclass = new Class(UUID.randomUUID()).addSuperClassRelation(relation);
      classRepository.save(subclass);
    }

    assertThat(classRepository.count()).isEqualTo(11);
    assertThat(classRepository.findSubclasses(cls, repository).count()).isEqualTo(10);
  }

  @Test
  void findRootClassesByRepository() {
    Repository repository = new Repository("repo");
    repository = repositoryRepository.save(repository);

    Class superClass = new Class(UUID.randomUUID()).setRepositoryId(repository.getId());
    Class subClass =
        new Class(UUID.randomUUID())
            .setRepositoryId(repository.getId())
            .addSuperClassRelation(new ClassRelation(superClass, repository.getId(), 1));

    classRepository.saveAll(Arrays.asList(superClass, subClass));

    Set<Class> rootClasses = classRepository.findRootClassesByRepository(repository);
    assertThat(rootClasses.size()).isEqualTo(1);
    assertThat(rootClasses.stream().findFirst().isPresent()).isTrue();
    assertThat(rootClasses.stream().findFirst().get().getId()).isEqualTo(superClass.getId());
  }
}
