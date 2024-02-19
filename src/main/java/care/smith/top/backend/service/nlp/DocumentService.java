package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import care.smith.top.top_document_query.adapter.TextAdapter;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DocumentService implements ContentService {
  @Autowired private DocumentNodeRepository documentNodeRepository;
  @Autowired private DocumentQueryService documentQueryService;

  @Cacheable("documentCount")
  public long count() {
    return documentQueryService.getTextAdapterConfigs().stream()
        .map(
            c -> {
              try {
                TextAdapter adapter = TextAdapter.getInstance(c);
                return adapter.count();
              } catch (Exception e) {
                return 0L;
              }
            })
        .reduce(Long::sum)
        .orElse(0L);
  }

  public List<Document> getDocumentsForConcepts(Set<String> conceptIds, Boolean exemplarOnly) {
    if (conceptIds.isEmpty()) {
      return List.of();
    }
    return documentNodeRepository
        .getDocumentsForConcepts(List.copyOf(conceptIds), exemplarOnly)
        .stream()
        .map(DocumentNodeEntity::toApiModel)
        .collect(Collectors.toList());
  }
}
