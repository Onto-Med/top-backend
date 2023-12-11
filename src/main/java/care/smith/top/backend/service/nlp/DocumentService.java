package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.elasticsearch.DocumentEntity;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.elasticsearch.DocumentRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import java.util.*;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class DocumentService implements ContentService {

  private final DocumentRepository documentRepository;
  private final DocumentNodeRepository documentNodeRepository;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  public DocumentService(
      DocumentRepository documentRepository, DocumentNodeRepository documentNodeRepository) {
    this.documentRepository = documentRepository;
    this.documentNodeRepository = documentNodeRepository;
  }

  @Override
  @Cacheable("documentCount")
  public long count() {
    return documentRepository.count();
  }

  // ### method calls for the Spring ES Repository

  /**
   * @param batchSize size of each list element returned from the stream
   * @return A stream consisting of lists with size 'batchSize'
   */
  public Stream<List<Document>> getAllDocumentsBatched(Integer batchSize) {
    return Stream.generate(
            new Supplier<List<Document>>() {
              int page = 0;

              @Override
              public List<Document> get() {
                Page<DocumentEntity> documentEntityPage =
                    documentRepository.findAll(PageRequest.of(page++, batchSize));
                return documentEntityPage.map(DocumentEntity::toApiModel).stream()
                    .collect(Collectors.toList());
              }
            })
        .takeWhile(list -> !list.isEmpty());
  }

  public Document getDocumentById(@NonNull String documentId) {
    DocumentEntity document = documentRepository.findById(documentId).orElse(null);
    if (document != null) {
      return document.toApiModel();
    } else {
      return DocumentEntity.nullDocument();
    }
  }

  /**
   * @param page Integer; if negative, method returns all entries as one page
   * @return Paged Document entries
   */
  public Page<Document> getAllDocuments(Integer page) {
    if (page < 0)
      return documentRepository.findAll(Pageable.unpaged()).map(DocumentEntity::toApiModel);
    return documentRepository.findAll(pageRequestOf(page)).map(DocumentEntity::toApiModel);
  }

  public Page<Document> getDocumentsByName(@NonNull String documentName, Integer page) {
    return documentRepository
        .findDocumentEntitiesByDocumentNameContains(documentName, pageRequestOf(page))
        .map(DocumentEntity::toApiModel);
  }

  public Page<Document> getDocumentsByIds(@NonNull Collection<String> ids, Integer page) {
    // ToDo: something's not working with the repository constructed methods; so I needed to
    //  implement my own filtering and paging

    // Page<DocumentEntity> documentPageEntity =
    // documentRepository.findDocumentEntitiesByIdIn(ids, pageRequestOf(page));
    //    Page<Document> documentPage =  documentPageEntity.map(DocumentEntity::toApiModel);
    Spliterator<DocumentEntity> documentEntitySpliterator =
        documentRepository.findAllById(ids).spliterator();
    int documentCount = (int) documentEntitySpliterator.getExactSizeIfKnown();
    List<Document> documents =
        StreamSupport.stream(documentEntitySpliterator, false)
            .map(DocumentEntity::toApiModel)
            .skip((long) (page - 1) * pageSize)
            .limit(pageSize)
            .collect(Collectors.toList());
    return new PageImpl<>(documents, pageRequestOf(page), documentCount);
  }

  public Page<Document> getDocumentsByPhrases(@NonNull Collection<String> phrases, Integer page) {
    return documentRepository
        .findDocumentEntitiesByDocumentTextIn(phrases, pageRequestOf(page))
        .map(DocumentEntity::toApiModel);
  }

  public Page<Document> getDocumentsByIdsAndPhrases(
      @NonNull Collection<String> ids, @NonNull Collection<String> phrases, Integer page) {
    return documentRepository
        .findDocumentEntitiesByIdInAndDocumentTextIn(ids, phrases, pageRequestOf(page))
        .map(DocumentEntity::toApiModel);
  }

  public List<Document> getDocumentsByTerms(String[] terms, String[] fields) {
    return documentRepository.getESDocumentsByTerms(terms, fields).stream()
        .map(DocumentEntity::toApiModel)
        .collect(Collectors.toList());
  }

  // ### method calls for the custom ES repository

  public List<Document> getDocumentsByTermsBoolean(
      String[] mustTerms, String[] shouldTerms, String[] notTerms, String[] fields) {
    return documentRepository
        .getESDocumentsByTermsBoolean(shouldTerms, mustTerms, notTerms, fields)
        .stream()
        .map(DocumentEntity::toApiModel)
        .collect(Collectors.toList());
  }

  public List<Document> getDocumentsByPhrases(String[] phrases, String[] fields) {
    return documentRepository.getESDocumentsByPhrases(phrases, fields).stream()
        .map(DocumentEntity::toApiModel)
        .collect(Collectors.toList());
  }

  public List<Document> getDocumentsByPhrasesBoolean(
      String[] mustPhrases, String[] shouldPhrases, String[] notPhrases, String[] fields) {
    return documentRepository
        .getESDocumentsByPhrasesBoolean(shouldPhrases, mustPhrases, notPhrases, fields)
        .stream()
        .map(DocumentEntity::toApiModel)
        .collect(Collectors.toList());
  }

  // ### method calls for the Document Node Repository (i.e. graph database)

  public List<Document> getDocumentsForConcepts(Set<String> conceptIds, Boolean exemplarOnly) {
    if (conceptIds.size() == 0) {
      return List.of();
    }
    return documentNodeRepository
        .getDocumentsForConcepts(List.copyOf(conceptIds), exemplarOnly)
        .stream()
        .map(DocumentNodeEntity::toApiModel)
        .collect(Collectors.toList());
  }

  // ### helper functions

  private PageRequest pageRequestOf(Integer page) {
    return PageRequest.of((page == null || page <= 0) ? 0 : page - 1, pageSize < 1 ? 1 : pageSize);
  }
}
