package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.service.nlp.DocumentQueryService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentPage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {
  @Autowired private DocumentService documentService;
  @Autowired private DocumentQueryService documentQueryService;

  @Override
  public ResponseEntity<List<Document>> getDocumentIdsByConceptClusterIds(
      List<String> conceptId, String gatheringMode, String name, Boolean exemplarOnly) {
    // ToDo: filter by 'name' not implemented
    if (!Objects.equals(gatheringMode, "intersection")) {
      return new ResponseEntity<>(
          documentService.getDocumentsForConcepts(Set.copyOf(conceptId), exemplarOnly),
          HttpStatus.OK);
    } else {
      Map<String, Document> hashMapDocuments = new HashMap<>();
      List<Set<String>> listOfSets = new ArrayList<>();
      ListIterator<String> it = conceptId.listIterator();

      while (it.hasNext()) {
        int idx = it.nextIndex();
        List<Document> documentList =
            documentService.getDocumentsForConcepts(Set.of(it.next()), exemplarOnly);
        for (Document doc : documentList) {
          hashMapDocuments.put(doc.getId(), doc);
        }
        listOfSets.add(documentList.stream().map(Document::getId).collect(Collectors.toSet()));
      }
      return new ResponseEntity<>(
          listOfSets.stream()
              .skip(1)
              .collect(() -> listOfSets.get(0), Set::retainAll, Set::retainAll)
              .stream()
              .map(hashMapDocuments::get)
              .collect(Collectors.toList()),
          HttpStatus.OK);
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
                      documentQueryService.getDocumentIds(organisationId, repositoryId, queryId),
                      page)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ResponseEntity<Document> getDocumentById(
      String documentId, List<String> conceptIds, List<String> include) {
    throw new NotImplementedException();
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
  }

  @Override
  public ResponseEntity<DocumentPage> getDocumentsByPhraseIds(
      String phraseId, List<String> include, String name, Integer page) {
    return DocumentApiDelegate.super.getDocumentsByPhraseIds(phraseId, include, name, page);
  }

  @Override
  public ResponseEntity<DocumentPage> getDocuments(
      List<String> include, List<String> phraseText, List<String> documentIds, Integer page) {
    throw new NotImplementedException();
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
  }
}
