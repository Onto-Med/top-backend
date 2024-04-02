package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentGatheringMode;
import care.smith.top.model.DocumentPage;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import care.smith.top.top_document_query.adapter.TextAdapter;
import care.smith.top.top_document_query.elasticsearch.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {
  private final Logger LOGGER = Logger.getLogger(DocumentApiDelegateImpl.class.getName());

  private final DocumentService documentService;

  public DocumentApiDelegateImpl(DocumentService documentService) {
    this.documentService = documentService;
  }

  @Override
  public ResponseEntity<DocumentPage> getDocuments(
      String dataSource, String name, List<String> documentIds, List<String> phraseIds, List<String> conceptClusterIds,
      List<String> phraseText, DocumentGatheringMode gatheringMode, Boolean exemplarOnly,
      Integer page, List<String> include)
  {
    page -= 1;
    TextAdapter adapter;
    try {
      adapter = documentService.getAdapterForDataSource(dataSource);
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    exemplarOnly = (exemplarOnly != null) ? exemplarOnly : false;

    // Neo4j filters are 'phraseIds', 'conceptClusterIds', 'phraseText'
    boolean neo4jFilterOn = false;
    HashSet<String> finalDocumentIds = new HashSet<>();
    if (phraseIds != null && !phraseIds.isEmpty()) {
      finalDocumentIds.addAll(
        documentService
          .getDocumentsForPhraseIds(Set.copyOf(phraseIds), exemplarOnly).stream()
          .map(Document::getId)
          .collect(Collectors.toSet())
      );
      neo4jFilterOn = true;
    }
    if (conceptClusterIds != null && !conceptClusterIds.isEmpty()) {
      // 'gatheringMode' is only relevant for getting documents by 'conceptClusterIds'
      Set<String> conceptClusterIdSet = documentService
          .getDocumentsForConceptIds(Set.copyOf(conceptClusterIds), exemplarOnly, gatheringMode).stream()
          .map(Document::getId)
          .collect(Collectors.toSet());
      if (neo4jFilterOn) {
        finalDocumentIds.retainAll(conceptClusterIdSet);
      } else { finalDocumentIds.addAll(conceptClusterIdSet); }
      neo4jFilterOn = true;
    }
    if (phraseText != null && !phraseText.isEmpty()) {
      Set<String> phraseTextSet = documentService
          .getDocumentsForPhraseTexts(Set.copyOf(phraseText), exemplarOnly).stream()
          .map(Document::getId)
          .collect(Collectors.toSet());
      if (neo4jFilterOn) {
        finalDocumentIds.retainAll(phraseTextSet);
      } else { finalDocumentIds.addAll(phraseTextSet); }
      neo4jFilterOn = true;
    }

    boolean esFilterOn = !(documentIds == null || documentIds.isEmpty());
    Page<Document> documentPage;
    try {
      if (!neo4jFilterOn && !esFilterOn) {
        if (name == null || name.trim().isEmpty()) {
          documentPage = adapter.getAllDocuments(page);
        } else {
          //ToDo: wildcard '*' is hard-coded into ElasticSearchAdapter method, so right now 'name' becomes 'name*'
          documentPage = adapter.getDocumentsByName(name, page);
        }
      } else if (!neo4jFilterOn) {
        documentPage = adapter.getDocumentsByIds(Set.copyOf(documentIds), page);
      } else if (!esFilterOn) {
        documentPage = adapter.getDocumentsByIds(finalDocumentIds, page);
      } else {
        finalDocumentIds.retainAll(documentIds);
        documentPage = adapter.getDocumentsByIds(finalDocumentIds, page);
      }
    } catch (IOException e) {
      LOGGER.fine("Server Instance could not be reached/queried.");
      return ResponseEntity.of(Optional.of(new DocumentPage()));
    }
    if (documentPage != null) {
      return ResponseEntity.ok(ApiModelMapper.toDocumentPage(documentPage));
    }
    return ResponseEntity.ok(new DocumentPage());
  }

  @Override
  public ResponseEntity<Document> getSingleDocumentById(String documentId, String dataSource, List<String> include) {
    TextAdapter adapter;
    try {
      adapter = documentService.getAdapterForDataSource(dataSource);
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    try {
      return ResponseEntity.ok(adapter.getDocumentById(documentId).orElseThrow());
    } catch (IOException e) {
      LOGGER.fine("Server Instance could not be reached/queried.");
      return ResponseEntity.of(Optional.ofNullable(DocumentEntity.nullDocument()));
    } catch (NoSuchElementException e) {
      LOGGER.fine(String.format("No document found with id: '%s'", documentId));
      return ResponseEntity.of(Optional.ofNullable(DocumentEntity.nullDocument()));
    }
  }

  @Override
  public ResponseEntity<DocumentPage> getDocumentsForQuery(
      String organisationId, String repositoryId, UUID queryId, Integer page) {
    page -= 1;
    TextAdapter adapter;
    try {
      adapter = documentService.getAdapterFromQuery(organisationId, repositoryId, queryId);
    } catch (NoSuchElementException e) {
      LOGGER.severe(String.format(
          "The text adapter for 'organisation: %s', 'repository: %s' and 'query: %s' could not be found.",
          organisationId, repositoryId, queryId.toString())
      );
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    try {
      return ResponseEntity.ok(
          ApiModelMapper.toDocumentPage(
              adapter.getDocumentsByIds(
                  documentService.getDocumentIdsForQuery(organisationId, repositoryId, queryId), page)
          )
      );
    } catch (IOException e) {
      LOGGER.fine("Server Instance could not be reached/queried.");
      return ResponseEntity.ok(new DocumentPage());
    }
  }
}
