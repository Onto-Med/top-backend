package care.smith.top.backend.nlp.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import care.smith.top.backend.api.nlp.DocumentApiDelegateImpl;
import care.smith.top.backend.util.AbstractNLPTest;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentGatheringMode;
import care.smith.top.model.DocumentPage;
import care.smith.top.top_document_query.adapter.DocumentReference;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
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
  private final String exampleDatasource = "exampledatasource";

  @BeforeAll
  static void setUp() throws IOException, InstantiationException {
    var service = mockedDocumentService();
    var adapter = service.getAdapterForDataSource("exampledatasource");
    var config = new TextAdapterConfig();
    config.setId("exampledatasource");
    when(adapter.getConfig()).thenReturn(config);
    when(adapter.forIndex("index")).thenReturn(adapter);
    when(adapter.getDocument(any(), anyBoolean()))
        .thenAnswer(
            invocation -> {
              DocumentReference reference = invocation.getArgument(0);
              return adapter.getDocumentById(reference.documentId(), invocation.getArgument(1));
            });
    documentApi = new DocumentApiDelegateImpl(service);
  }

  @Test
  void getDocuments() {
    ResponseEntity<DocumentPage> response1 =
        documentApi.getDocuments(
            exampleDatasource, "index", null, null, null, null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1_2, Set.copyOf(Objects.requireNonNull(response1.getBody()).getContent()));

    ResponseEntity<DocumentPage> response2 =
        documentApi.getDocuments(
            exampleDatasource, "index", "Document", null, null, null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1_2, Set.copyOf(Objects.requireNonNull(response2.getBody()).getContent()));

    ResponseEntity<DocumentPage> response3 =
        documentApi.getDocuments(
            exampleDatasource, "index", null, List.of("d1"), null, null, null, null, null, 0, null);
    Assertions.assertEquals(
        documents1, Set.copyOf(Objects.requireNonNull(response3.getBody()).getContent()));

    ResponseEntity<DocumentPage> response4 =
        documentApi.getDocuments(
            exampleDatasource,
            "index",
            null,
            null,
            List.of("p1", "p2"),
            null,
            null,
            null,
            null,
            0,
            null);
    Assertions.assertEquals(
        documents1_2, Set.copyOf(Objects.requireNonNull(response4.getBody()).getContent()));

    ResponseEntity<DocumentPage> response5 =
        documentApi.getDocuments(
            exampleDatasource,
            "index",
            null,
            null,
            List.of("p1", "p2"),
            null,
            null,
            null,
            true,
            0,
            null);
    Assertions.assertEquals(
        documents2, Set.copyOf(Objects.requireNonNull(response5.getBody()).getContent()));

    ResponseEntity<DocumentPage> response6 =
        documentApi.getDocuments(
            exampleDatasource,
            "index",
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
            exampleDatasource,
            "index",
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
        documentApi.getSingleDocumentById("d1", exampleDatasource, "index", null, null, null);
    Assertions.assertEquals(documents1, Set.of(Objects.requireNonNull(response1.getBody())));
    ResponseEntity<Document> response2 =
        documentApi.getSingleDocumentById("d2", exampleDatasource, "index", null, null, null);
    Assertions.assertEquals(documents2, Set.of(Objects.requireNonNull(response2.getBody())));
  }
}
