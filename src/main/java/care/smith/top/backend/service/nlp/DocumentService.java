package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.elasticsearch.DocumentEntity;
import care.smith.top.backend.model.jpa.OrganisationDao_;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.elasticsearch.DocumentRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import java.util.*;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.print.Doc;

@Service
public class DocumentService implements ContentService {

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  private final DocumentRepository documentRepository;
  private final DocumentNodeRepository documentNodeRepository;

  private PageRequest pageRequestOf(Integer page) {
    return PageRequest.of(page == null ? 1 : page - 1, pageSize);
  }

  @Autowired
  public DocumentService(
      DocumentRepository documentRepository, DocumentNodeRepository documentNodeRepository) {
    this.documentRepository = documentRepository;
    this.documentNodeRepository = documentNodeRepository;
  }

  @Override
  @Cacheable("documentCount")
  public long count() {
    return documentNodeRepository.count();
  }

  // ### method calls for the Spring ES Repository

  public Document getDocumentById(
      @NonNull String documentId) {
    DocumentEntity document = documentRepository.findById(documentId).orElse(null);
    if (document != null) {
      return document.toApiModel();
    } else {
      return document.nullDocument();
    }
  }

  public Page<Document> getAllDocuments(Integer page){
    return documentRepository
        .findAll(pageRequestOf(page))
        .map(DocumentEntity::toApiModel);
  }

  public Page<Document> getDocumentsByName(
      @NonNull String documentName, Integer page) {
    return documentRepository
        .findDocumentEntitiesByDocumentNameContains(documentName, pageRequestOf(page))
        .map(DocumentEntity::toApiModel);
  }

  public Page<Document> getDocumentsByIds(
      @NonNull Collection<String> ids, Integer page) {
    return documentRepository
        .findDocumentEntitiesByIdIn(ids, pageRequestOf(page))
        .map(DocumentEntity::toApiModel);
  }

  public Page<Document> getDocumentsByPhrases(
      @NonNull Collection<String> phrases, Integer page) {
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

  // ### method calls for the custom ES repository

  public List<Document> getDocumentsByTerms(String[] terms, String[] fields) {
    return documentRepository.getESDocumentsByTerms(terms, fields).stream()
        .map(DocumentEntity::toApiModel)
        .collect(Collectors.toList());
  }

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
}
