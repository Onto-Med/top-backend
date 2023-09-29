package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.model.Document;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(SpringExtension.class)
class DocumentServiceTest extends AbstractNLPTest {

  @BeforeEach
  void testIsContainerRunning() {
    assertTrue(elasticsearchContainer.isRunning());
  }

  @Test
  void getDocumentByName() {
    List<String> documentName =
        documentService.getDocumentsByName("test01", 1).stream()
            .map(Document::getName)
            .collect(Collectors.toList());
    assertNotNull(documentName);
    assertArrayEquals(new String[] {"test01"}, documentName.toArray());
  }

  @Test
  void getDocumentsByTerms() {
    List<Document> documents_with_term_document =
        documentService.getDocumentsByTerms(new String[] {"document"}, new String[] {"text"});
    assertEquals(3, documents_with_term_document.size());

    List<Document> documents_with_term_entity =
        documentService.getDocumentsByTerms(new String[] {"entity"}, new String[] {"text"});
    assertEquals(1, documents_with_term_entity.size());

    List<Document> documents_with_term_more_entities =
        documentService.getDocumentsByTerms(
            new String[] {"entity", "entities"}, new String[] {"text"});
    assertEquals(2, documents_with_term_more_entities.size());

    List<Document> documents_with_term_fuzzy_entity =
        documentService.getDocumentsByTerms(new String[] {"entitie~"}, new String[] {"text"});
    assertEquals(2, documents_with_term_fuzzy_entity.size());

    assertEquals(documents_with_term_fuzzy_entity, documents_with_term_more_entities);
  }

  @Test
  void getDocumentsByTermsBoolean() {
    List<Document> documents_with_term_boolean1 =
        documentService.getDocumentsByTermsBoolean(
            new String[] {"document"}, // must
            new String[] {""}, // should
            new String[] {"entitie~"}, // not
            new String[] {"text"});
    assertEquals(1, documents_with_term_boolean1.size());

    List<Document> documents_with_term_boolean2 =
        documentService.getDocumentsByTermsBoolean(
            new String[] {""}, // must
            new String[] {""}, // should
            new String[] {"nothing", "two"}, // not
            new String[] {"text"});
    assertEquals(1, documents_with_term_boolean2.size());
  }

  @Test
  void getDocumentsForConcepts() {}
}
