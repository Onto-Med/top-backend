package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ClassVersionRepositoryTest extends RepositoryTest {
  @Autowired ClassRepository classRepository;
  @Autowired ClassVersionRepository classVersionRepository;

  @Test
  void getCurrentSuperClassVersionsByOwnerId() {
    Repository repository = new Repository();

    ClassVersion superClassVersion = new ClassVersion().setVersion(1);
    Class superClass =
        new Class().setCurrentVersion(superClassVersion).setRepositoryId(repository.getId());

    ClassVersion subClassVersion = new ClassVersion().setVersion(1);
    Class subClass =
        new Class()
            .setCurrentVersion(subClassVersion)
            .setRepositoryId(repository.getId())
            .addSuperClass(superClass, repository.getId(), 1);

    assertThatCode(() -> classRepository.saveAll(Arrays.asList(superClass, subClass)))
        .doesNotThrowAnyException();

    assertThat(
            classVersionRepository.getCurrentSuperClassVersionsByOwnerId(
                subClass, repository.getId()))
        .isNotEmpty()
        .allMatch(c -> c.getaClass().getId().equals(superClass.getId()))
        .size()
        .isEqualTo(1);
  }

  @Test
  void findByClassIdAndVersion() {
    Class cls = new Class();
    Annotation annotation =
        (Annotation)
            new Annotation("title", "example title", "en")
                .addAnnotation(new Annotation("prefix", "EN", "en"));
    ClassVersion classVersion =
        (ClassVersion)
            new ClassVersion()
                .setVersion(classRepository.getNextVersion(cls))
                .addAnnotation(annotation)
                .addAnnotation(new Annotation("description", "example description", "de"));
    cls.setCurrentVersion(classVersion);

    classRepository.save(cls);

    assertThat(
            classVersionRepository.findByClassIdAndVersion(cls.getId(), classVersion.getVersion()))
        .isPresent()
        .hasValueSatisfying(
            cv -> {
              assertThat(cv.getaClass().getId()).isEqualTo(cls.getId());
              assertThat(cv.getVersion()).isEqualTo(1);
              assertThat(cv.getAnnotations()).size().isEqualTo(2);

              assertThat(cv.getAnnotations("description").stream().findFirst())
                  .isPresent()
                  .hasValueSatisfying(
                      a -> {
                        assertThat(a.getProperty()).isEqualTo("description");
                        assertThat(a.getLanguage()).isEqualTo("de");
                        assertThat(a.getStringValue()).isEqualTo("example description");
                      });

              assertThat(cv.getAnnotations("title").stream().findFirst())
                  .isPresent()
                  .hasValueSatisfying(
                      a -> {
                        assertThat(a.getProperty()).isEqualTo("title");
                        assertThat(a.getLanguage()).isEqualTo("en");
                        assertThat(a.getStringValue()).isEqualTo("example title");
                        assertThat(a.getAnnotations("prefix").stream().findFirst())
                            .isPresent()
                            .hasValueSatisfying(
                                a2 -> {
                                  assertThat(a2.getProperty()).isEqualTo("prefix");
                                  assertThat(a2.getLanguage()).isEqualTo("en");
                                  assertThat(a2.getStringValue()).isEqualTo("EN");
                                });
                      });

              assertThat(cv.getAnnotations("prefix").stream().findFirst()).isNotPresent();
            });
  }

  @Test
  void findCurrentByClassId() {
    Class cls = new Class().setCurrentVersion(new ClassVersion().setVersion(1));
    assertThat(classRepository.save(cls)).isNotNull();
    cls.setCurrentVersion(new ClassVersion().setVersion(classRepository.getNextVersion(cls)));
    assertThat(classRepository.save(cls)).isNotNull();

    Slice<ClassVersion> result =
        classVersionRepository.findByClassId(cls.getId(), PageRequest.ofSize(10));
    assertThat(result).size().isEqualTo(2);

    assertThat(classVersionRepository.findCurrentByClassId(cls.getId()))
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(2));
  }

  @Test
  void getPrevious() {
    ClassVersion previous = new ClassVersion().setVersion(1);
    Class cls = new Class().setCurrentVersion(previous);
    assertThat(classRepository.save(cls)).isNotNull();

    ClassVersion current =
        new ClassVersion()
            .setVersion(classRepository.getNextVersion(cls))
            .setPreviousVersion(previous);
    cls.setCurrentVersion(current);
    assertThat(classRepository.save(cls)).isNotNull();

    Optional<ClassVersion> classVersion = classVersionRepository.findCurrentByClassId(cls.getId());
    assertThat(classVersion)
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(2));

    assertThat(classVersionRepository.getPrevious(classVersion.get()))
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(1));
  }

    @Test
    void getNext() {
        ClassVersion previous = new ClassVersion().setVersion(1);
        Class cls = new Class().setCurrentVersion(previous);
        assertThat(classRepository.save(cls)).isNotNull();

        ClassVersion current =
                new ClassVersion()
                        .setVersion(classRepository.getNextVersion(cls))
                        .setPreviousVersion(previous);
        cls.setCurrentVersion(current);
        assertThat(classRepository.save(cls)).isNotNull();

        classRepository.setCurrent(cls, previous);

        Optional<ClassVersion> classVersion = classVersionRepository.findCurrentByClassId(cls.getId());
        assertThat(classVersion)
                .isPresent()
                .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(1));

        assertThat(classVersionRepository.getNext(classVersion.get()))
                .isPresent()
                .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(2));
    }
}
