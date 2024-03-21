package care.smith.top.backend.service.nlp;

import care.smith.top.backend.AbstractNLPTest;

import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

@SpringBootTest
class DocumentServiceTest extends AbstractNLPTest {
  //ToDo: should the corresponding methods for which the tests are disabled also be tested?

  @Test
  void documentNodesCount() {
    assertThat(documentNodeRepository.count()).isEqualTo(Long.valueOf(2));
  }

  @Test
  void getDocumentsForConceptIds() {
    assertThat(
        documentNodeRepository.getDocumentsForConceptIds(Set.of("c2"), false)
            .stream()
            .map(DocumentNodeEntity::toApiModel)
            .collect(Collectors.toSet())
    ).isEqualTo(documents1_2);

    assertThat(
        documentNodeRepository.getDocumentsForConceptIds(Set.of("c2"), true)
            .stream()
            .map(DocumentNodeEntity::toApiModel)
            .collect(Collectors.toSet())
    ).isEqualTo(Set.of());
  }

  @Test
  void getDocumentsForPhraseIds() {
    assertThat(
        documentNodeRepository.getDocumentsForPhraseIds(Set.of("p1", "p2"), true)
            .stream()
            .map(DocumentNodeEntity::toApiModel)
            .collect(Collectors.toSet())
    ).isEqualTo(documents2);
  }

  @Test
  void getDocumentsForPhraseTexts() {
    assertThat(
        documentNodeRepository.getDocumentsForPhrasesText(Set.of("phrase"), false)
            .stream()
            .map(DocumentNodeEntity::toApiModel)
            .collect(Collectors.toSet())
    ).isEqualTo(documents1_2);

    assertThat(
        documentNodeRepository.getDocumentsForPhrasesText(Set.of("there", "here"), true)
            .stream()
            .map(DocumentNodeEntity::toApiModel)
            .collect(Collectors.toSet())
    ).isEqualTo(documents2);
  }

  @Test
  @Disabled
  void getDocumentIdsForQuery() {
  }

  @Test
  @Disabled
  void textAdaptersDocumentCount() {
  }

  @Test
  @Disabled
  void getAdapterForDataSource() {
  }

  @Test
  @Disabled
  void getAdapterFromQuery() {
  }
}
