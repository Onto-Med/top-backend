package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentGatheringMode;
import care.smith.top.top_document_query.adapter.TextAdapter;
import java.io.IOException;
import java.util.*;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DocumentService implements ContentService {
  private DocumentNodeRepository documentNodeRepository;
  private DocumentQueryService documentQueryService;

  public DocumentService(
      DocumentNodeRepository documentNodeRepository, DocumentQueryService documentQueryService) {
    this.documentNodeRepository = documentNodeRepository;
    this.documentQueryService = documentQueryService;
  }

  public void setDocumentQueryService(DocumentQueryService documentQueryService) {
    this.documentQueryService = documentQueryService;
  }

  public void setDocumentNodeRepository(DocumentNodeRepository documentNodeRepository) {
    this.documentNodeRepository = documentNodeRepository;
  }

  public DocumentNodeRepository getDocumentNodeRepository() {
    return documentNodeRepository;
  }

  public DocumentQueryService getDocumentQueryService() {
    return documentQueryService;
  }

  @Cacheable("documentCount")
  public long count() {
    return documentNodeRepository.count();
  }

  public long textAdaptersDocumentCount() {
    return getDocumentQueryService().getTextAdapterConfigs().stream()
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

  public List<Document> getDocumentsForConceptIds(
      Set<String> conceptIds, String corpusId, Boolean exemplarOnly) {
    if (conceptIds == null || conceptIds.isEmpty()) {
      return List.of();
    }
    return getDocumentNodeRepository()
        .getDocumentsForConceptIds(conceptIds, corpusId, exemplarOnly)
        .stream()
        .map(DocumentNodeEntity::toApiModel)
        .collect(Collectors.toList());
  }

  public List<Document> getDocumentsForConceptIds(
      Set<String> conceptIds,
      String corpusId,
      Boolean exemplarOnly,
      DocumentGatheringMode gatheringMode) {
    if (conceptIds == null || conceptIds.isEmpty()) {
      return List.of();
    }

    gatheringMode = (gatheringMode != null) ? gatheringMode : DocumentGatheringMode.UNION;

    if (Objects.equals(gatheringMode, DocumentGatheringMode.INTERSECTION)) {
      Map<String, DocumentNodeEntity> hashMapDocuments = new HashMap<>();
      List<Set<String>> listOfSets = new ArrayList<>();
      conceptIds.forEach(
          conceptId -> {
            List<DocumentNodeEntity> dneList =
                getDocumentNodeRepository()
                    .getDocumentsForConceptIds(Set.of(conceptId), corpusId, exemplarOnly);
            listOfSets.add(
                dneList.stream().map(DocumentNodeEntity::documentId).collect(Collectors.toSet()));
            dneList.forEach(dne -> hashMapDocuments.put(dne.documentId(), dne));
          });
      return listOfSets.stream()
          .skip(1)
          .collect(() -> listOfSets.get(0), Set::retainAll, Set::retainAll)
          .stream()
          .map(hashMapDocuments::get)
          .map(DocumentNodeEntity::toApiModel)
          .collect(Collectors.toList());
    } else if (Objects.equals(gatheringMode, DocumentGatheringMode.EXCLUSIVE)) {
      return getDocumentNodeRepository()
          .getDocumentsForConceptIds(
              Collections.singleton(conceptIds.iterator().next()), corpusId, exemplarOnly)
          .stream()
          .map(DocumentNodeEntity::toApiModel)
          .collect(Collectors.toList());
    } else {
      return getDocumentNodeRepository()
          .getDocumentsForConceptIds(conceptIds, corpusId, exemplarOnly)
          .stream()
          .map(DocumentNodeEntity::toApiModel)
          .collect(Collectors.toList());
    }
  }

  public List<Document> getDocumentsForPhraseIds(
      Set<String> phraseIds, String corpusId, Boolean exemplarOnly) {
    if (phraseIds == null || phraseIds.isEmpty()) {
      return List.of();
    }
    return getDocumentNodeRepository()
        .getDocumentsForPhraseIds(phraseIds, corpusId, exemplarOnly)
        .stream()
        .map(DocumentNodeEntity::toApiModel)
        .collect(Collectors.toList());
  }

  public List<Document> getDocumentsForPhraseTexts(
      Set<String> phraseTexts, String corpusId, Boolean exemplarOnly) {
    if (phraseTexts == null || phraseTexts.isEmpty()) {
      return List.of();
    }
    return getDocumentNodeRepository()
        .getDocumentsForPhrasesText(phraseTexts, corpusId, exemplarOnly)
        .stream()
        .map(DocumentNodeEntity::toApiModel)
        .collect(Collectors.toList());
  }

  @Cacheable("dataSourceAdapter")
  public TextAdapter getAdapterForDataSource(String dataSource) throws InstantiationException {
    return TextAdapter.getInstance(
        getDocumentQueryService().getTextAdapterConfig(dataSource).orElseThrow());
  }

  @Cacheable("queryAdapter")
  public TextAdapter getAdapterFromQuery(String organisationId, String repositoryId, UUID queryId)
      throws NoSuchElementException {
    return getDocumentQueryService()
        .getTextAdapter(organisationId, repositoryId, queryId)
        .orElseThrow();
  }

  public List<String> getDocumentIdsForQuery(
      String organisationId, String repositoryId, UUID queryId) throws IOException {
    return new ArrayList<>(
        getDocumentQueryService()
            .getDocumentIdsAndOffsets(organisationId, repositoryId, queryId)
            .keySet());
  }
}
