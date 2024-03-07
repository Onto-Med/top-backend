package care.smith.top.backend.api.nlp;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentGatheringMode;
import care.smith.top.model.DocumentPage;
import care.smith.top.top_document_query.adapter.TextAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentApiDelegateImplTest extends AbstractNLPTest {
  private DocumentApiDelegateImpl documentApi;

  @BeforeEach
  void setUp() throws IOException, InstantiationException {
    documentApi = new DocumentApiDelegateImpl(mockedDocumentService());
  }

  @Test
  void getDocuments() {
    Set<Document> documents1 = Set.of(
        new Document().id("d1").name("Document 1"));
    Set<Document> documents2 = Set.of(
        new Document().id("d2").name("Document 2"));
    Set<Document> documents1_2 = Set.of(
        new Document().id("d1").name("Document 1"), new Document().id("d2").name("Document 2"));

    ResponseEntity<DocumentPage> response1 =  documentApi.getDocuments(
        "exampleDataSource", null, null, null, null, null,
        null, null, 0, null);
    Assertions.assertEquals(documents1_2, Set.copyOf(Objects.requireNonNull(response1.getBody()).getContent()));

    ResponseEntity<DocumentPage> response2 =  documentApi.getDocuments(
        "exampleDataSource", "Document", null, null, null, null,
        null, null, 0, null);
    Assertions.assertEquals(documents1_2, Set.copyOf(Objects.requireNonNull(response2.getBody()).getContent()));

    ResponseEntity<DocumentPage> response3 =  documentApi.getDocuments(
        "exampleDataSource", null, List.of("d1"), null, null, null,
        null, null, 0, null);
    Assertions.assertEquals(documents1, Set.copyOf(Objects.requireNonNull(response3.getBody()).getContent()));

    ResponseEntity<DocumentPage> response4 =  documentApi.getDocuments(
        "exampleDataSource", null, null, List.of("p1", "p2"), null, null,
        null, null, 0, null);
    Assertions.assertEquals(documents1_2, Set.copyOf(Objects.requireNonNull(response4.getBody()).getContent()));

    ResponseEntity<DocumentPage> response5 =  documentApi.getDocuments(
        "exampleDataSource", null, null, List.of("p1", "p2"), null, null,
        null, true, 0, null);
    Assertions.assertEquals(documents2, Set.copyOf(Objects.requireNonNull(response5.getBody()).getContent()));

    ResponseEntity<DocumentPage> response6 =  documentApi.getDocuments(
        "exampleDataSource", null, null, null, List.of("c1", "c2"), null,
        DocumentGatheringMode.INTERSECTION, null, 0, null);
    Assertions.assertEquals(documents2, Set.copyOf(Objects.requireNonNull(response6.getBody()).getContent()));

    ResponseEntity<DocumentPage> response7 =  documentApi.getDocuments(
        "exampleDataSource", null, List.of("d1"), null, List.of("c1", "c2"), null,
        DocumentGatheringMode.UNION, null, 0, null);
    Assertions.assertEquals(documents1, Set.copyOf(Objects.requireNonNull(response7.getBody()).getContent()));
  }

  @Test
  void getSingleDocumentById() {
  }

  @Test
  void getDocumentsForQuery() {
  }

  private static DocumentService mockedDocumentService() throws IOException, InstantiationException {
    PageImpl<Document> page1 = new PageImpl<>(List.of(new Document().id("d1").name("Document 1")));
    PageImpl<Document> page2 = new PageImpl<>(List.of(new Document().id("d2").name("Document 2")));
    PageImpl<Document> page1_2 = new PageImpl<>(
        List.of(new Document().id("d1").name("Document 1"), new Document().id("d2").name("Document 2")));
    DocumentNodeEntity d1 = new DocumentNodeEntity("d1", "Document 1", Set.of());
    DocumentNodeEntity d2 = new DocumentNodeEntity("d2", "Document 2", Set.of());

    TextAdapter adapter = mock(TextAdapter.class);
    when(adapter.getDocumentById("d1"))
        .thenReturn(Optional.ofNullable(new Document().id("d1").name("Document 1")));
    when(adapter.getDocumentById("d2"))
        .thenReturn(Optional.ofNullable(new Document().id("d2").name("Document 2")));
    when(adapter.getDocumentsByIds(eq(Set.of("d1", "d2")), anyInt())).thenReturn(page1_2);
    when(adapter.getDocumentsByIds(eq(Set.of("d1")), anyInt())).thenReturn(page1);
    when(adapter.getDocumentsByIds(eq(Set.of("d2")), anyInt())).thenReturn(page2);
    when(adapter.getAllDocuments(anyInt())).thenReturn(page1_2);
    when(adapter.getDocumentsByName(eq("Document 1"), anyInt())).thenReturn(page1);
    when(adapter.getDocumentsByName(eq("Document 2"), anyInt())).thenReturn(page2);
    when(adapter.getDocumentsByName(eq("Document*"), anyInt())).thenReturn(page1_2);

    DocumentNodeRepository documentNodeRepository = mock(DocumentNodeRepository.class);
    when(documentNodeRepository.getDocumentsForPhraseIds(Set.of("p1", "p2"), false))
        .thenReturn(List.of(d1, d2));
    when(documentNodeRepository.getDocumentsForPhraseIds(Set.of("p1", "p2"), true))
        .thenReturn(List.of(d2));
    when(documentNodeRepository.getDocumentsForConceptIds(Set.of("c1", "c2"), false))
        .thenReturn(List.of(d1, d2));
    when(documentNodeRepository.getDocumentsForConceptIds(Set.of("c1"), false))
        .thenReturn(List.of(d2));
    when(documentNodeRepository.getDocumentsForConceptIds(Set.of("c2"), false))
        .thenReturn(List.of(d1, d2));

    DocumentService documentService = mock(DocumentService.class);
    when(documentService.getDocumentNodeRepository()).thenReturn(documentNodeRepository);
    when(documentService.getAdapterFromQuery(anyString(), anyString(), any()))
        .thenReturn(adapter);
    when(documentService.getAdapterForDataSource(anyString()))
        .thenReturn(adapter);
    when(documentService.getDocumentIdsForQuery(anyString(), anyString(), any()))
        .thenReturn(List.of("d1", "d2"));
    when(documentService.getDocumentsForConceptIds(anySet(), anyBoolean())).thenCallRealMethod();
    when(documentService.getDocumentsForConceptIds(anySet(), anyBoolean(), any())).thenCallRealMethod();
    when(documentService.getDocumentsForPhraseIds(anySet(), anyBoolean())).thenCallRealMethod();
    when(documentService.getDocumentsForPhraseTexts(anySet(), anyBoolean())).thenCallRealMethod();

    return documentService;
  }
}