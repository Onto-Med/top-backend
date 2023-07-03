package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import care.smith.top.model.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
class DocumentServiceTest extends AbstractNLPTest {
    @Container
    private static final ElasticsearchContainer elasticsearchContainer = new DocumentElasticsearchContainer();

//    @Autowired
//    private ElasticsearchTemplate template;

    @BeforeAll
    static void setUp() {
        elasticsearchContainer.start();
    }

    @AfterAll
    static void destroy() {
        elasticsearchContainer.stop();
    }

    @BeforeEach
    void testIsContainerRunning() {
        assertTrue(elasticsearchContainer.isRunning());
//        recreateIndex();
    }

    // TODO: provide this example data via elasticsearch testcontainers
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

//    private void recreateIndex() {
//        if (template.indexOps(DocumentEntity.class).exists()) {
//            template.indexOps(DocumentEntity.class).delete();
//            template.indexOps(DocumentEntity.class).create();
//        }
//    }
}