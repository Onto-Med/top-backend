package care.smith.top.backend.service.nlp;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.backend.AbstractNLPTest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocumentServiceTest extends AbstractNLPTest {
  private static DocumentService documentService;

  @BeforeEach
  void provideDocumentService() throws IOException, InstantiationException {
    documentService =
        new DocumentService(
            documentNodeRepository, mockedDocumentService().getDocumentQueryService());
  }

  @Test
  void documentNodesCount() {
    assertThat(documentService.count()).isEqualTo(Long.valueOf(2));
  }

  @Test
  void getDocumentsForConceptIds() {
    assertThat(new HashSet<>(documentService.getDocumentsForConceptIds(Set.of("c2"), false)))
        .isEqualTo(documents1_2);

    assertThat(new HashSet<>(documentService.getDocumentsForConceptIds(Set.of("c2"), true)))
        .isEqualTo(Set.of());
  }

  @Test
  void getDocumentsForPhraseIds() {
    assertThat(new HashSet<>(documentService.getDocumentsForPhraseIds(Set.of("p1", "p2"), true)))
        .isEqualTo(documents2);
  }

  @Test
  void getDocumentsForPhraseTexts() {
    assertThat(new HashSet<>(documentService.getDocumentsForPhraseTexts(Set.of("phrase"), false)))
        .isEqualTo(documents1_2);

    assertThat(
            new HashSet<>(
                documentService.getDocumentsForPhraseTexts(Set.of("there", "here"), true)))
        .isEqualTo(documents2);
  }

  @Test
  @Disabled
  void getDocumentIdsForQuery() {
    // As of now: No reason to test here, as it just calls a method in DocumentQueryService
  }

  @Test
  @Disabled
  void getAdapterForDataSource() {
    // As of now: No reason to test here, as it just calls a method in DocumentQueryService
  }

  @Test
  @Disabled
  void getAdapterFromQuery() {
    // As of now: No reason to test here, as it just calls a method in DocumentQueryService
  }
}
