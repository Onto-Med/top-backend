package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentServiceTest extends AbstractNLPTest {

    @Test
    void getDocumentByName() {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentName("testDoc0");
        documentEntity.setDocumentText("What do we have here? A text with an important Entity. Maybe even a proper Term.");
        documentRepository.save(documentEntity);

        assertEquals(documentService.getDocumentByName("testDoc0").getId(), "testDoc0");
    }

    @Test
    void getDocumentsByTerms() {
    }

    @Test
    void getDocumentsForConcepts() {
    }
}