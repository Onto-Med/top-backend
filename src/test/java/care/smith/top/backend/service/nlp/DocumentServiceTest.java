package care.smith.top.backend.service.nlp;

import care.smith.top.model.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentServiceTest extends AbstractNLPTest {

    // Since indexing from within Spring Apps did not work on creation time of Test Cases,
    // there are these three documents prepared on a test index within the proper ES instance:
    //      {"id": "01", "name": "test01", "text": "What do we have here? A test document. With an entity. Nice."},
    //      {"id": "02", "name": "test02", "text": "Another document is here. It has two entities."},
    //      {"id": "03", "name": "test03", "text": "And a third document; but this one features nothing."}

    @Test
    void getDocumentByName() {
        String documentName = documentService.getDocumentByName("test01").getName();
        assertNotNull(documentName);
        assertEquals("test01", documentName);
    }

    @Test
    void getDocumentsByTerms() {
        List<Document> documents_with_term_document = documentService
                .getDocumentsByTerms(new String[]{"document"}, new String[]{"text"});
        assertEquals(3, documents_with_term_document.size());

        List<Document> documents_with_term_entity = documentService
                .getDocumentsByTerms(new String[]{"entity"}, new String[]{"text"});
        assertEquals(1, documents_with_term_entity.size());

        List<Document> documents_with_term_more_entities = documentService
                .getDocumentsByTerms(new String[]{"entity", "entities"}, new String[]{"text"});
        assertEquals(2, documents_with_term_more_entities.size());

        List<Document> documents_with_term_fuzzy_entity = documentService
                .getDocumentsByTerms(new String[]{"entitie~"}, new String[]{"text"});
        assertEquals(2, documents_with_term_fuzzy_entity.size());

        assertEquals(documents_with_term_fuzzy_entity, documents_with_term_more_entities);
    }

    @Test
    void getDocumentsByTermsBoolean() {
        List<Document> documents_with_term_boolean1 = documentService
                .getDocumentsByTermsBoolean(
                        new String[]{"document"},   // must
                        new String[]{""},           // should
                        new String[]{"entitie~"},   // not
                        new String[]{"text"});
        assertEquals(1, documents_with_term_boolean1.size());

        List<Document> documents_with_term_boolean2 = documentService
                .getDocumentsByTermsBoolean(
                        new String[]{""},                 // must
                        new String[]{""},                 // should
                        new String[]{"nothing", "two"},   // not
                        new String[]{"text"});
        assertEquals(1, documents_with_term_boolean2.size());
    }

    @Test
    void getDocumentsForConcepts() {
    }
}