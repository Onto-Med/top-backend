package care.smith.top.backend.nlp.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import care.smith.top.backend.api.DocumentApiController;
import care.smith.top.backend.api.nlp.DocumentApiDelegateImpl;
import care.smith.top.backend.api.nlp.DocumentSelectionExceptionHandler;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentImport;
import care.smith.top.model.QueryType;
import care.smith.top.top_document_query.adapter.DocumentReference;
import care.smith.top.top_document_query.adapter.IndexSelection;
import care.smith.top.top_document_query.adapter.TextAdapter;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentSelectionTest {
  private final DocumentService service = mock(DocumentService.class);
  private final TextAdapter root = mock(TextAdapter.class);
  private final TextAdapter scoped = mock(TextAdapter.class);
  private final TextAdapterConfig config = new TextAdapterConfig();
  private final DocumentApiDelegateImpl delegate = new DocumentApiDelegateImpl(service);
  private MockMvc mvc;

  @BeforeEach
  void setUp() throws Exception {
    config.setConfigVersion(2);
    config.setId("source");
    config.setIndex(new String[] {"allowed"});
    when(service.getAdapterForDataSource("source")).thenReturn(root);
    when(root.forIndex(anyString()))
        .thenAnswer(
            invocation -> {
              IndexSelection.select(config, List.of(invocation.getArgument(0))).singleIndex();
              return scoped;
            });
    when(scoped.getAllDocumentsPaged(anyInt(), anyBoolean())).thenReturn(Page.empty());
    mvc =
        MockMvcBuilders.standaloneSetup(new DocumentApiController(delegate))
            .setControllerAdvice(new DocumentSelectionExceptionHandler())
            .build();
  }

  @Test
  void requiresIndexEvenForOneChoice() throws Exception {
    mvc.perform(get("/document").param("dataSource", "source"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
    verifyNoInteractions(root);
  }

  @Test
  void rejectsBlankListAndDisallowedSelections() throws Exception {
    for (String index : List.of("", " ", "allowed,other", "[allowed]", "other")) {
      mvc.perform(get("/document").param("dataSource", "source").param("index", index))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value(400));
    }
    verifyNoInteractions(scoped);
  }

  @Test
  void rejectsRepeatedIndexEvenWhenValuesAreIdentical() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("index", "allowed", "allowed");
    ReflectionTestUtils.setField(delegate, "request", request);
    mvc.perform(get("/document").param("dataSource", "source").param("index", "allowed", "allowed"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
    verifyNoInteractions(root);
  }

  @Test
  void explicitChoiceUsesScopedAdapterWithoutChangingConfiguration() throws Exception {
    mvc.perform(get("/document").param("dataSource", "source").param("index", "allowed"))
        .andExpect(status().isOk());
    verify(scoped).getAllDocumentsPaged(0, true);
    verify(root, never()).getAllDocumentsPaged(anyInt(), anyBoolean());
    assertArrayEquals(new String[] {"allowed"}, config.getIndex());
  }

  @Test
  void importForwardsExplicitDestinationWithoutRequiringResponseProvenance() {
    Document document = new Document().id("id").text("text");
    when(root.importDocuments(eq("allowed"), any(Document[].class), isNull()))
        .thenReturn(new DocumentImport());
    delegate.importDocuments("source", "allowed", null, List.of(document));
    verify(root).importDocuments("allowed", new Document[] {document}, null);
    verify(root, never()).importDocuments(any(Document[].class), any());
  }

  @Test
  void exactLookupUsesReferenceRatherThanScopingToConcreteBackingIndex() throws Exception {
    when(root.getConfig()).thenReturn(config);
    DocumentReference reference = new DocumentReference("source", "backing-index", "id");
    when(root.getDocument(reference, false)).thenReturn(Optional.empty());
    delegate.getSingleDocumentById("id", "source", "backing-index", null, null, null);
    verify(root).getDocument(reference, false);
    verify(root, never()).forIndex(anyString());
  }

  @Test
  void discoveryExposesOnlyConfiguredChoicesAlongsideExistingMetadata() {
    DocumentQueryService queries = spy(new DocumentQueryService());
    doReturn(List.of(config)).when(queries).getTextAdapterConfigs();
    var source = queries.getDataSources().getFirst();
    assertEquals("source", source.getId());
    assertEquals(QueryType.CONCEPT, source.getQueryType());
    assertEquals(Set.of("allowed"), source.getIndices());
  }
}
