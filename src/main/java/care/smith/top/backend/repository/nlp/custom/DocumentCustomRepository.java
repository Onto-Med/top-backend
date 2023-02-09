package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;

import java.util.List;

public interface DocumentCustomRepository {
    List<DocumentEntity> getDocumentsByTerms(String[] terms, String[] fields);
}
