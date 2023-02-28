package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

public interface DocumentCustomRepository {

    /**
     *
     * @param terms list of terms; can also be boolean term description (e.g. [+TERM1, -TERM2])
     * @param fields list of fields to search in
     * @return List of DocumentEntity
     */
    List<DocumentEntity> getDocumentsByTerms(String[] terms, String[] fields);

    List<DocumentEntity> getDocumentsByQuery(String query);
}
