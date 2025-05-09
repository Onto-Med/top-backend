package care.smith.top.backend.nlp.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.backend.nlp.AbstractNLPTest;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.Document;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocumentServiceTest extends AbstractNLPTest {
  private static DocumentService documentService;
  private final Set<String> documentIds1 =
      documents1.stream().map(Document::getId).collect(Collectors.toSet());
  private final Set<String> documentIds2 =
      documents2.stream().map(Document::getId).collect(Collectors.toSet());
  private final Set<String> documentIds1_2 =
      documents1_2.stream().map(Document::getId).collect(Collectors.toSet());
  private final String exampleDatasource = "exampledatasource";

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
    assertThat(
            new HashSet<>(
                documentService
                    .getDocumentsForConceptIds(Set.of("c2"), exampleDatasource, false)
                    .stream()
                    .map(Document::getId)
                    .collect(Collectors.toSet())))
        .isEqualTo(documentIds1_2);

    assertThat(
            new HashSet<>(
                documentService
                    .getDocumentsForConceptIds(Set.of("c2"), exampleDatasource, true)
                    .stream()
                    .map(Document::getId)
                    .collect(Collectors.toSet())))
        .isEqualTo(Set.of());
  }

  @Test
  void getDocumentsForPhraseIds() {
    assertThat(
            new HashSet<>(
                documentService
                    .getDocumentsForPhraseIds(Set.of("p1", "p2"), exampleDatasource, true)
                    .stream()
                    .map(Document::getId)
                    .collect(Collectors.toSet())))
        .isEqualTo(documentIds2);
  }

  @Test
  void getDocumentsForPhraseTexts() {
    assertThat(
            new HashSet<>(
                documentService
                    .getDocumentsForPhraseTexts(Set.of("phrase"), exampleDatasource, false)
                    .stream()
                    .map(Document::getId)
                    .collect(Collectors.toSet())))
        .isEqualTo(documentIds1_2);

    assertThat(
            new HashSet<>(
                documentService
                    .getDocumentsForPhraseTexts(Set.of("there", "here"), exampleDatasource, true)
                    .stream()
                    .map(Document::getId)
                    .collect(Collectors.toSet())))
        .isEqualTo(documentIds2);
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
