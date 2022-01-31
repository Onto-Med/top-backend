package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ClassVersionRepositoryTest extends RepositoryTest {
  @Autowired ClassRepository classRepository;
  @Autowired ClassVersionRepository classVersionRepository;

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
                .addAnnotation(annotation)
                .addAnnotation(new Annotation("description", "example description", "de"));
    cls.createVersion(classVersion, true);

    classRepository.save(cls);

    assertThat(
            classVersionRepository.findByClassIdAndVersion(cls.getId(), classVersion.getVersion()))
        .isPresent()
        .hasValueSatisfying(
            cv -> {
              assertThat(cv.getaClass().getId()).isEqualTo(cls.getId());
              assertThat(cv.getVersion()).isEqualTo(classVersion.getVersion());
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
}
