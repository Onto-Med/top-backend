package care.smith.top.backend.repository.elasticsearch;

import care.smith.top.backend.model.elasticsearch.DocumentEntity;
import care.smith.top.backend.repository.elasticsearch.custom.DocumentCustomRepository;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository
    extends ElasticsearchRepository<DocumentEntity, String>, DocumentCustomRepository {

  Page<DocumentEntity> findDocumentEntitiesByDocumentNameContains(
      String documentName, Pageable page);

  Page<DocumentEntity> findDocumentEntitiesByIdIn(Collection<String> ids, Pageable page);

  Page<DocumentEntity> findDocumentEntitiesByDocumentTextIn(
      Collection<String> phrases, Pageable page);

  Page<DocumentEntity> findDocumentEntitiesByIdInAndDocumentTextIn(
      Collection<String> ids, Collection<String> phrases, Pageable page);
}
