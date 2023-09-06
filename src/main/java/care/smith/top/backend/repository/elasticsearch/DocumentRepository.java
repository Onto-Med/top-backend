package care.smith.top.backend.repository.elasticsearch;

import care.smith.top.backend.model.elasticsearch.DocumentEntity;
import care.smith.top.backend.repository.elasticsearch.custom.DocumentCustomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DocumentRepository
    extends ElasticsearchRepository<DocumentEntity, String>, DocumentCustomRepository {

  DocumentEntity findDocumentEntityByDocumentName(String documentName);

  Page<DocumentEntity> findDocumentEntitiesByIdIn(Collection<String> id, Pageable page);
}
