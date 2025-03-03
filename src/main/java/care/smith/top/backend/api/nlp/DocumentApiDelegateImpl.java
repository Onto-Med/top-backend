package care.smith.top.backend.api.nlp;

import static java.util.regex.Pattern.UNICODE_CASE;

import care.smith.top.backend.api.DocumentApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentGatheringMode;
import care.smith.top.model.DocumentPage;
import care.smith.top.model.Phrase;
import care.smith.top.top_document_query.adapter.TextAdapter;
import care.smith.top.top_document_query.elasticsearch.DocumentEntity;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {
  private final Logger LOGGER = Logger.getLogger(DocumentApiDelegateImpl.class.getName());
  @Autowired private PhraseService phraseService;
  private final DocumentService documentService;
  private final String COLOR_PRE = "$color::";
  private final String COLOR_AFTER = "::color$";
  private final String BORDER_MARKUP =
      "<span style=\"border: 2px solid black; padding: 3px; border-radius: 5px\">";
  private final Map<Character, String> REGEX_SPECIAL =
      Map.ofEntries(
          Map.entry('.', "\\."),
          Map.entry('+', "\\+"),
          Map.entry('*', "\\*"),
          Map.entry('?', "\\?"),
          Map.entry('^', "\\^"),
          Map.entry('$', "\\$"),
          Map.entry('(', "\\("),
          Map.entry(')', "\\)"),
          Map.entry('[', "\\["),
          Map.entry(']', "\\]"),
          Map.entry('{', "\\{"),
          Map.entry('}', "\\}"),
          Map.entry('|', "\\|"),
          Map.entry('\\', "\\\\"));

  public DocumentApiDelegateImpl(DocumentService documentService) {
    this.documentService = documentService;
  }

  @Override
  public ResponseEntity<DocumentPage> getDocuments(
      String dataSource,
      String name,
      List<String> documentIds,
      List<String> phraseIds,
      List<String> conceptClusterIds,
      List<String> phraseText,
      DocumentGatheringMode gatheringMode,
      Boolean exemplarOnly,
      Integer page,
      List<String> include) {
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
          documentService.getDocumentsForPhraseIds(Set.copyOf(phraseIds), exemplarOnly).stream()
              .map(Document::getId)
              .collect(Collectors.toSet()));
      neo4jFilterOn = true;
    }
    if (conceptClusterIds != null && !conceptClusterIds.isEmpty()) {
      // 'gatheringMode' is only relevant for getting documents by 'conceptClusterIds'
      Set<String> conceptClusterIdSet =
          documentService
              .getDocumentsForConceptIds(Set.copyOf(conceptClusterIds), exemplarOnly, gatheringMode)
              .stream()
              .map(Document::getId)
              .collect(Collectors.toSet());
      if (neo4jFilterOn) {
        finalDocumentIds.retainAll(conceptClusterIdSet);
      } else {
        finalDocumentIds.addAll(conceptClusterIdSet);
      }
      neo4jFilterOn = true;
    }
    if (phraseText != null && !phraseText.isEmpty()) {
      Set<String> phraseTextSet =
          documentService.getDocumentsForPhraseTexts(Set.copyOf(phraseText), exemplarOnly).stream()
              .map(Document::getId)
              .collect(Collectors.toSet());
      if (neo4jFilterOn) {
        finalDocumentIds.retainAll(phraseTextSet);
      } else {
        finalDocumentIds.addAll(phraseTextSet);
      }
      neo4jFilterOn = true;
    }

    boolean esFilterOn = !(documentIds == null || documentIds.isEmpty());
    Page<Document> documentPage;
    try {
      if (!neo4jFilterOn && !esFilterOn) {
        if (name == null || name.trim().isEmpty()) {
          documentPage = adapter.getAllDocumentsPaged(page, true);
        } else {
          // ToDo: wildcard '*' is hard-coded into ElasticSearchAdapter method, so right now 'name'
          // becomes 'name*'
          documentPage = adapter.getDocumentsByNamePaged(name, page, true);
        }
      } else if (!neo4jFilterOn) {
        documentPage = adapter.getDocumentsByIdsPaged(Set.copyOf(documentIds), page, true);
      } else if (!esFilterOn) {
        documentPage = adapter.getDocumentsByIdsPaged(finalDocumentIds, page, true);
      } else {
        finalDocumentIds.retainAll(documentIds);
        documentPage = adapter.getDocumentsByIdsPaged(finalDocumentIds, page, true);
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
  public ResponseEntity<Document> getSingleDocumentById(
      String documentId,
      String dataSource,
      List<String> highlightConcepts,
      List<String> offsets,
      List<String> include) {
    TextAdapter adapter;
    try {
      adapter = documentService.getAdapterForDataSource(dataSource);
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    try {
      Document document = adapter.getDocumentById(documentId, false).orElseThrow();
      if (highlightConcepts == null) highlightConcepts = new ArrayList<>();
      if (offsets == null) offsets = new ArrayList<>();

      String[] colors = new String[] {"yellow", "black"};
      String conceptId = null;

      addBorderToHighlights(document, offsets);

      for (String concept : highlightConcepts) {
        if (concept.startsWith(COLOR_PRE)) {
          colors =
              concept.substring(COLOR_PRE.length(), concept.lastIndexOf(COLOR_AFTER)).split("\\|");
          conceptId = concept.substring(concept.lastIndexOf(COLOR_AFTER) + COLOR_AFTER.length());
        } else {
          conceptId = concept;
        }
        String finalConceptId = conceptId;

        buildTextWithHighlights(
            document,
            colors,
            phraseService.getPhrasesForConcept(finalConceptId).stream()
                .map(Phrase::getText)
                .collect(Collectors.toList()));
      }
      return ResponseEntity.ok(document);
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
      LOGGER.severe(
          String.format(
              "The text adapter for 'organisation: %s', 'repository: %s' and 'query: %s' could not"
                  + " be found.",
              organisationId, repositoryId, queryId.toString()));
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    try {
      return ResponseEntity.ok(
          ApiModelMapper.toDocumentPage(
              adapter.getDocumentsByIdsPaged(
                  documentService.getDocumentIdsForQuery(organisationId, repositoryId, queryId),
                  page,
                  true)));
    } catch (IOException e) {
      LOGGER.fine("Server Instance could not be reached/queried.");
      return ResponseEntity.ok(new DocumentPage());
    }
  }

  private void addBorderToHighlights(Document document, List<String> offsets) {
    String highlightTag = BORDER_MARKUP + "%s</span>";
    StringBuilder highlightBuilder = new StringBuilder();
    int curr = 0;
    for (String offset : offsets) {
      int begin = Integer.parseInt(offset.split("-")[0]);
      int end = Integer.parseInt(offset.split("-")[1]);
      highlightBuilder.append(document.getHighlightedText(), curr, begin);
      highlightBuilder.append(
          String.format(highlightTag, document.getHighlightedText().substring(begin, end)));
      curr = end;
    }
    highlightBuilder.append(document.getHighlightedText().substring(curr));
    document.setHighlightedText(highlightBuilder.toString());
  }

  private void buildTextWithHighlights(Document document, String[] colors, List<String> terms) {
    // ToDo: surrounding es highlights span with concept cluster highlight span needs to be seen if
    // it works for all cases
    String colorBackgroundValue = colors[0];
    String colorForegroundValue = colors.length > 1 ? colors[1] : "black";
    String markTag =
        "<span style=\"background: %s; color: %s; padding: 2px; border-radius: 5px\">%s</span>";
    if (terms == null) return;
    for (String mark : terms) {
      StringBuilder escapedString = new StringBuilder();
      for (char character : mark.toCharArray()) {
        if (REGEX_SPECIAL.containsKey(character)) {
          escapedString.append(REGEX_SPECIAL.get(character));
        } else {
          escapedString.append(character);
        }
      }
      String repl =
          Pattern.compile(
                  String.format("\\b(%s)?%s(</span>)?\\b", BORDER_MARKUP, escapedString),
                  Pattern.CASE_INSENSITIVE | UNICODE_CASE)
              .matcher(document.getHighlightedText())
              .replaceAll(
                  matchResult ->
                      String.format(
                          markTag,
                          colorBackgroundValue,
                          colorForegroundValue,
                          document
                              .getHighlightedText()
                              .substring(matchResult.start(), matchResult.end())));
      document.highlightedText(repl);
    }
  }
}
