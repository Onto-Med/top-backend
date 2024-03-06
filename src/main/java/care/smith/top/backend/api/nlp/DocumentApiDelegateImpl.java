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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {
  private final Logger LOGGER = Logger.getLogger(DocumentApiDelegateImpl.class.getName());

  @Autowired private DocumentService documentService;
  @Autowired private DocumentQueryService documentQueryService;

  @Override
  public ResponseEntity<DocumentPage> getDocuments(
      String dataSource, String name, List<String> documentIds, List<String> phraseIds, List<String> conceptClusterIds,
      List<String> phraseText, DocumentGatheringMode gatheringMode, Boolean exemplarOnly,
      Integer page, List<String> include)
  {
    TextAdapter adapter;
    try {
      adapter = TextAdapter.getInstance(
          documentQueryService
              .getTextAdapterConfig(dataSource)
              .orElseThrow());
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    // Neo4j filters are 'phraseIds', 'conceptClusterIds', 'phraseText'
    boolean neo4jFilter = false;
    Set<String> phraseIdFilter = Set.of();
    boolean phraseFilterOn = false;
    if (phraseIds != null && !phraseIds.isEmpty()) {
      phraseIdFilter = documentService
          .getDocumentsForPhraseIds(Set.copyOf(phraseIds), exemplarOnly).stream()
          .map(Document::getId)
          .collect(Collectors.toSet());
      neo4jFilter = true;
      phraseFilterOn = true;
    }
    Set<String> conceptClusterIdFilter = Set.of();
    boolean conceptClusterFilterOn = false;
    if (conceptClusterIds != null && !conceptClusterIds.isEmpty()) {
      // 'gatheringMode' is only relevant for getting documents by 'conceptClusterIds'
      conceptClusterIdFilter = documentService
          .getDocumentsForConceptIds(Set.copyOf(conceptClusterIds), exemplarOnly, gatheringMode).stream()
          .map(Document::getId)
          .collect(Collectors.toSet());
      neo4jFilter = true;
      conceptClusterFilterOn = true;
    }
    Set<String> phraseTextFilter = Set.of();
    boolean phraseTextFilterOn = false;
    if (phraseText != null && !phraseText.isEmpty()) {
      phraseTextFilter = documentService
          .getDocumentsForPhraseTexts(Set.copyOf(phraseText), exemplarOnly).stream()
          .map(Document::getId)
          .collect(Collectors.toSet());
      neo4jFilter = true;
      phraseTextFilterOn = true;
    }

    boolean esFilter = false;
    Set<String> finalDocumentIds;
    if (neo4jFilter) {
      finalDocumentIds = adapter.getAllDocumentsBatched(20).forEach(dList -> dList.stream().map(Document::getId)
          .filter(conceptClusterIdFilter::contains)
          .filter(phraseTextFilter::contains)
          .filter((documentIds == null || documentIds.isEmpty()) ? e -> true : documentIds::contains)
          .collect(Collectors.toSet());
    } else {
      if (documentIds == null || documentIds.isEmpty()) {
        finalDocumentIds = Set.of();
      } else {
        finalDocumentIds = Set.copyOf(documentIds);
        esFilter = true;
      }
    }

    Page<Document> documentPage = Page.empty();
    try {
      if (!esFilter && !neo4jFilter) {
        if (name == null || name.trim().isEmpty()) {
          documentPage = adapter.getAllDocuments(page);
        } else {
          //ToDo: should the wildcard be optional?
          documentPage = adapter.getDocumentsByName(name + "*", page);
        }
      } else {
        // At this point, there should be ids in finalDocumentIds, since it's than either
        // - just a copy of a non-empty 'documentIds' ==> esFilter == true
        // - or a set filtered by all ('documentIds', 'phraseIds', 'conceptClusterIds', 'phraseText') ==> neo4jFilter == true
        documentPage = adapter.getDocumentsByIds(finalDocumentIds, page);
      }
    } catch (IOException e) {
      LOGGER.fine("Couldn't get documents: " + e.getMessage());
    }
    return ResponseEntity.ok(ApiModelMapper.toDocumentPage(documentPage));
  }

  @Override
  public ResponseEntity<Document> getSingleDocumentById(String documentId, String dataSource, List<String> include) {
    try {
      return ResponseEntity.ok(
          TextAdapter.getInstance(
            documentQueryService
                .getTextAdapterConfig(dataSource)
                .orElseThrow())
              .getDocumentById(documentId)
              .orElseThrow()
      );
    } catch (InstantiationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ResponseEntity<DocumentPage> getDocumentsForQuery(
      String organisationId, String repositoryId, UUID queryId, Integer page) {
    try {
      return ResponseEntity.ok(
          ApiModelMapper.toDocumentPage(
              documentQueryService
                  .getTextAdapter(organisationId, repositoryId, queryId)
                  .orElseThrow()
                  .getDocumentsByIds(
                      documentQueryService.getDocumentIds(organisationId, repositoryId, queryId),  //ToDo: should be cached?
                      page)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

//  @SafeVarargs
//  private Set<String> idsForGatheringMode(DocumentGatheringMode mode, Set<String>... idSets) {
//    if (Objects.equals(mode, DocumentGatheringMode.INTERSECTION)) {
//      List<Set<String>> listOfSets = Arrays.stream(idSets).collect(Collectors.toList());
//      return new HashSet<>(Arrays.stream(idSets)
//          .skip(1)
//          .collect(() -> listOfSets.get(0), Set::retainAll, Set::retainAll));
//    } else {
//      HashSet<String> finalIds = new HashSet<>();
//      Arrays.stream(idSets).forEach(finalIds::addAll);
//      return finalIds;
//    }
//  }
//  @Override
//  public ResponseEntity<List<Document>> getDocumentIdsByConceptClusterIds(
//      List<String> conceptId, String gatheringMode, String name, Boolean exemplarOnly) {
//    // ToDo: filter by 'name' not implemented
//    if (!Objects.equals(gatheringMode, "intersection")) {
//      return new ResponseEntity<>(
//          documentService.getDocumentsForConcepts(Set.copyOf(conceptId), exemplarOnly),
//          HttpStatus.OK);
//    } else {
//      Map<String, Document> hashMapDocuments = new HashMap<>();
//      List<Set<String>> listOfSets = new ArrayList<>();
//      ListIterator<String> it = conceptId.listIterator();
//
//      while (it.hasNext()) {
//        int idx = it.nextIndex();
//        List<Document> documentList =
//            documentService.getDocumentsForConcepts(Set.of(it.next()), exemplarOnly);
//        for (Document doc : documentList) {
//          hashMapDocuments.put(doc.getId(), doc);
//        }
//        listOfSets.add(documentList.stream().map(Document::getId).collect(Collectors.toSet()));
//      }
//      return new ResponseEntity<>(
//          listOfSets.stream()
//              .skip(1)
//              .collect(() -> listOfSets.get(0), Set::retainAll, Set::retainAll)
//              .stream()
//              .map(hashMapDocuments::get)
//              .collect(Collectors.toList()),
//          HttpStatus.OK);
//    }
//  }

//  @Override
//  public ResponseEntity<Document> getDocumentById(
//      String documentId, List<String> conceptIds, List<String> include) {
//    throw new NotImplementedException();
    // Todo: just testing
    //    if (conceptIds != null) {
    //      String[] conceptPhrases =
    //          conceptIds.stream()
    //              .map(
    //                  cid ->
    //                      phraseService.getPhrasesForConcept(cid).stream()
    //                          .map(Phrase::getText)
    //                          .collect(Collectors.joining("|")))
    //              .collect(Collectors.joining("|"))
    //              .split("\\|");
    //
    //      String[] shouldTerms =
    //          phraseService.getPhrasesForDocument(documentId, false).stream()
    //              .map(Phrase::getText)
    //              .filter(s -> Arrays.asList(conceptPhrases).contains(s))
    //              //                            .filter(s -> s.matches("[a-zA-Z]+"))
    //              .toArray(String[]::new);
    //
    //      List<Document> documents =
    //          documentService.getDocumentsByPhrases(shouldTerms, new String[] {"text"});
    //
    //      Optional<Document> document =
    //          documents.stream().filter(d -> d.getId().equals(documentId)).findFirst();
    //      return document
    //          .map(value -> new ResponseEntity<>(value, HttpStatus.OK))
    //          .orElseGet(
    //              () ->
    //                  new ResponseEntity<>(documentService.getDocumentById(documentId),
    // HttpStatus.OK));
    //    }
    //    return new ResponseEntity<>(documentService.getDocumentById(documentId), HttpStatus.OK);
//  }

//  @Override
//  public ResponseEntity<DocumentPage> getDocumentsByPhraseIds(
//      String phraseId, List<String> include, String name, Integer page) {
//    return DocumentApiDelegate.super.getDocumentsByPhraseIds(phraseId, include, name, page);
//  }
//
//  @Override
//  public ResponseEntity<DocumentPage> getDocuments(
//      List<String> include, List<String> phraseText, List<String> documentIds, Integer page) {
//    throw new NotImplementedException();
    //    Page<Document> documentPage;
    //    if (!(documentIds == null || documentIds.isEmpty())
    //        && (phraseText == null || phraseText.isEmpty())) {
    //      documentPage = documentService.getDocumentsByIds(documentIds, page);
    //    } else if ((documentIds == null || documentIds.isEmpty())
    //        && !(phraseText == null || phraseText.isEmpty())) {
    //      documentPage = documentService.getDocumentsByPhrases(phraseText, page);
    //    } else if (!(documentIds == null || documentIds.isEmpty())) {
    //      documentPage = documentService.getDocumentsByIdsAndPhrases(documentIds, phraseText,
    // page);
    //    } else {
    //      documentPage = documentService.getAllDocuments(page);
    //    }
    //
    //    if (documentPage == null) {
    //      return ResponseEntity.noContent().build();
    //    }
    //
    //    return ResponseEntity.ok(ApiModelMapper.toDocumentPage(documentPage));
//  }
}
