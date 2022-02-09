package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassRelation;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
    Class cls = new Class();
    Repository repository = new Repository();

    for (int i = 0; i < 10; i++) {
      ClassRelation relation =
          new ClassRelation().setSuperclass(cls).setOwnerId(repository.getId());
      Class subclass = new Class().addSuperClassRelation(relation);
      classRepository.save(subclass);
    }

    assertThat(classRepository.count()).isEqualTo(11);
    assertThat(classRepository.findSubclasses(cls.getId(), repository.getId()).count()).isEqualTo(10);
  }

  @Test
  void getNextVersion() {
    assertThat(classRepository.getNextVersion(null)).isEqualTo(1);

    Class cls = classRepository.save(new Class());
    assertThat(classRepository.getNextVersion(cls)).isEqualTo(1);

    ClassVersion classVersion = new ClassVersion().setVersion(1);
    cls.setCurrentVersion(classVersion);

    cls = classRepository.save(cls);
    assertThat(cls.getCurrentVersion())
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(1));

    assertThat(classRepository.getNextVersion(cls)).isEqualTo(2);
  }

  @Test
  void findByIdAndRepositoryId() {
    String repositoryId = UUID.randomUUID().toString();
    Class cls = new Class().setRepositoryId(repositoryId);
    classRepository.save(cls);

    assertThat(classRepository.findByIdAndRepositoryId(cls.getId(), repositoryId))
        .hasValueSatisfying(
            c -> {
              assertThat(c.getId()).isEqualTo(cls.getId());
              assertThat(c.getRepositoryId()).isEqualTo(repositoryId);
            });
  }

  @Test
  void findRootClassesByRepository() {
    Repository repository = new Repository();
    repository = repositoryRepository.save(repository);

    Class superClass = new Class().setRepositoryId(repository.getId());
    Class subClass =
        new Class()
            .setRepositoryId(repository.getId())
            .addSuperClassRelation(new ClassRelation(superClass, repository.getId(), 1));

    classRepository.saveAll(Arrays.asList(superClass, subClass));

    Set<Class> rootClasses = classRepository.findRootClassesByRepository(repository);
    assertThat(rootClasses.size()).isEqualTo(1);
    assertThat(rootClasses.stream().findFirst().isPresent()).isTrue();
    assertThat(rootClasses.stream().findFirst().get().getId()).isEqualTo(superClass.getId());
  }
}
