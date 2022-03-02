package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
  void findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType() {
    Repository repository = new Repository();

    Class cls1 =
        new Class()
            .setRepositoryId(repository.getId())
            .setCurrentVersion(
                (ClassVersion)
                    new ClassVersion()
                        .setHiddenAt(Instant.now())
                        .setVersion(1)
                        .addAnnotation(new Annotation("title", "test name", "en")));

    Class cls2 =
        new Class()
            .setRepositoryId(repository.getId())
            .setCurrentVersion(new ClassVersion().setName("example").setVersion(1));

    classRepository.saveAll(Arrays.asList(cls1, cls2));

    cls1.setCurrentVersion(
        (ClassVersion)
            new ClassVersion()
                .setName("example")
                .setVersion(2)
                .addAnnotation(new Annotation("title", "test name", "en"))
                .addAnnotation(new Annotation("type", "type", null))
                .addAnnotation(new Annotation("dataType", "decimal", null)));
    classRepository.save(cls1);

    Slice<ClassVersion> result =
        classVersionRepository.findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
            repository.getId(), "est", null, null, PageRequest.ofSize(10));

    assertThat(result).size().isEqualTo(1);

    assertThat(result.stream().findFirst())
        .hasValueSatisfying(
            cv -> {
              assertThat(cv.getHiddenAt()).isNull();
              assertThat(cv.getName()).isEqualTo("example");
              assertThat(cv.getAnnotations("title").stream().findFirst())
                  .hasValueSatisfying(t -> assertThat(t.getStringValue()).isEqualTo("test name"));
            });

    assertThat(
            classVersionRepository
                .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
                    repository.getId(), null, List.of("type"), null, PageRequest.ofSize(10))
                .getNumberOfElements())
        .isEqualTo(1);

    assertThat(
            classVersionRepository
                .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
                    repository.getId(), null, null, "decimal", PageRequest.ofSize(10))
                .getNumberOfElements())
        .isEqualTo(1);

    assertThat(
            classVersionRepository
                .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
                    repository.getId(), "test", List.of("type"), "decimal", PageRequest.ofSize(10))
                .getNumberOfElements())
        .isEqualTo(1);

    assertThat(
            classVersionRepository
                .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
                    repository.getId(), null, null, null, PageRequest.ofSize(10))
                .getNumberOfElements())
        .isEqualTo(2);

    assertThat(
            classVersionRepository
                .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
                    repository.getId(), "example", null, null, PageRequest.ofSize(10))
                .getNumberOfElements())
        .isEqualTo(2);
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
  void getPreviousUnhidden() {
    Class cls = new Class().setCurrentVersion(new ClassVersion().setVersion(1));
    assertThat(classRepository.save(cls)).isNotNull();

    cls.setCurrentVersion(new ClassVersion().setVersion(classRepository.getNextVersion(cls)));
    assertThat(classRepository.save(cls)).isNotNull();

    Optional<ClassVersion> classVersion = classVersionRepository.findCurrentByClassId(cls.getId());
    assertThat(classVersion)
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(2));

    assertThat(classVersionRepository.getPrevious(classVersion.get()))
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(1));
  }
}
