package care.smith.top.backend.repository.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import care.smith.top.backend.repository.nlp.custom.DocumentCustomRepository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends
        ElasticsearchRepository<DocumentEntity, String>,
        DocumentCustomRepository {

    DocumentEntity findDocumentEntityByDocumentName(String documentName);
}
