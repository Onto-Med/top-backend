package care.smith.top.backend.neo4j_ontology_access.repository;

import care.smith.top.backend.neo4j_ontology_access.model.Annotation;
import care.smith.top.backend.neo4j_ontology_access.model.ClassVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationRepositoryTest extends RepositoryTest {
  @Autowired AnnotationRepository annotationRepository;
  @Autowired ClassVersionRepository classVersionRepository;

  @Test
  void findAnnotationsByProperty() {
    ClassVersion classVersion =
        (ClassVersion)
            new ClassVersion(1L)
              .setName("test")
                .addAnnotation(new Annotation("title", "example", "en"))
                .addAnnotation(new Annotation("description", "some text", "de"));

    classVersion = classVersionRepository.save(classVersion);

    assertThat(classVersion.getAnnotations().size()).isEqualTo(2);
    assertThat(classVersion.getAnnotations("title").size()).isEqualTo(1);
    assertThat(classVersion.getAnnotations("description").size()).isEqualTo(1);

    assertThat(annotationRepository.findByClassVersionAndProperty(classVersion, "title").size())
        .isEqualTo(1);
  }
}
