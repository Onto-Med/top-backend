package care.smith.top.backend.api.nlp;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentGatheringMode;
import care.smith.top.model.DocumentPage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class DocumentApiDelegateImplTest extends AbstractNLPTest {
  private static DocumentApiDelegateImpl documentApi;

  @BeforeAll
  static void setUp() throws IOException, InstantiationException {
    documentApi = new DocumentApiDelegateImpl(mockedDocumentService());
  }

  @Test
  void getDocuments() {
    ResponseEntity<DocumentPage> response1 =
        documentApi.getDocuments(
            "exampleDataSource", null, null, null, null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1_2, Set.copyOf(Objects.requireNonNull(response1.getBody()).getContent()));

    ResponseEntity<DocumentPage> response2 =
        documentApi.getDocuments(
            "exampleDataSource", "Document", null, null, null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1_2, Set.copyOf(Objects.requireNonNull(response2.getBody()).getContent()));

    ResponseEntity<DocumentPage> response3 =
        documentApi.getDocuments(
            "exampleDataSource", null, List.of("d1"), null, null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1, Set.copyOf(Objects.requireNonNull(response3.getBody()).getContent()));

    ResponseEntity<DocumentPage> response4 =
        documentApi.getDocuments(
            "exampleDataSource", null, null, List.of("p1", "p2"), null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1_2, Set.copyOf(Objects.requireNonNull(response4.getBody()).getContent()));

    ResponseEntity<DocumentPage> response5 =
        documentApi.getDocuments(
            "exampleDataSource", null, null, List.of("p1", "p2"), null, null, null, true, 0, null);
    Assertions.assertEquals(
        documents2, Set.copyOf(Objects.requireNonNull(response5.getBody()).getContent()));

    ResponseEntity<DocumentPage> response6 =
        documentApi.getDocuments(
            "exampleDataSource",
            null,
            null,
            null,
            List.of("c1", "c2"),
            null,
            DocumentGatheringMode.INTERSECTION,
            null,
            0,
            null);
    Assertions.assertEquals(
        documents2, Set.copyOf(Objects.requireNonNull(response6.getBody()).getContent()));

    ResponseEntity<DocumentPage> response7 =
        documentApi.getDocuments(
            "exampleDataSource",
            null,
            List.of("d1"),
            null,
            List.of("c1", "c2"),
            null,
            DocumentGatheringMode.UNION,
            null,
            0,
            null);
    Assertions.assertEquals(
        documents1, Set.copyOf(Objects.requireNonNull(response7.getBody()).getContent()));
  }

  @Test
  void getSingleDocumentById() {
    ResponseEntity<Document> response1 =
        documentApi.getSingleDocumentById("d1", "exampleDataSource", null, null, null);
    Assertions.assertEquals(documents1, Set.of(Objects.requireNonNull(response1.getBody())));
    ResponseEntity<Document> response2 =
        documentApi.getSingleDocumentById("d2", "exampleDataSource", null, null, null);
    Assertions.assertEquals(documents2, Set.of(Objects.requireNonNull(response2.getBody())));
  }
}
