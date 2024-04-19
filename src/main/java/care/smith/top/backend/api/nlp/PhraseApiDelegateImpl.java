package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.PhraseApiDelegate;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.Document;
import care.smith.top.model.DocumentPage;
import care.smith.top.model.Phrase;
import care.smith.top.model.PhrasePage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import care.smith.top.top_document_query.adapter.TextAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PhraseApiDelegateImpl implements PhraseApiDelegate {
  //ToDo: check whether we want 'mostImportantOnly' and 'exactTextMatch' to be hardcoded boolean here or if we want
  // to adapt the API accordingly

  private final Logger LOGGER = Logger.getLogger(PhraseApiDelegateImpl.class.getName());
  private final PhraseService phraseService;
  private final DocumentService documentService;

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;

  public PhraseApiDelegateImpl(PhraseService phraseService, DocumentService documentService) {
    this.phraseService = phraseService;
    this.documentService = documentService;
  }

  @Override
  public ResponseEntity<PhrasePage> getPhrases(String text, String conceptClusterId, Integer page) {
    HashSet<Phrase> phraseSet = new HashSet<>();
    boolean textFilter = !(text == null || text.trim().isEmpty());
    boolean conceptClusterFilter = !(conceptClusterId == null || conceptClusterId.trim().isEmpty());

    if (!textFilter && !conceptClusterFilter) {
      phraseSet.addAll(phraseService.getAllPhrases());
    } else if (conceptClusterFilter && !textFilter) {
      phraseSet.addAll(phraseService.getPhrasesForConcept(conceptClusterId));
    } else if (!conceptClusterFilter) {
      phraseSet.addAll(phraseService.getPhraseByText(text, false));
    } else {
      phraseSet.addAll(phraseService.getPhrasesForConcept(conceptClusterId));
      phraseSet.retainAll(phraseService.getPhraseByText(text, false));
    }
    return ResponseEntity.ok(ApiModelMapper.toPhrasePage(pageFromSet(phraseSet, page)));
  }

  @Override
  public ResponseEntity<PhrasePage> getPhrasesByDocumentId(String documentId, String dataSource, List<String> include, String text, Integer page) {
    if ((documentId == null || documentId.trim().isEmpty()) || (dataSource == null || dataSource.trim().isEmpty())) {
      LOGGER.severe("Either 'documentId', 'dataSource' or both are missing.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    Optional<Document> document;
    try {
      TextAdapter adapter = documentService.getAdapterForDataSource(dataSource);
      document = adapter.getDocumentById(documentId, true);
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (IOException e) {
      LOGGER.fine("Server Instance could not be reached/queried.");
      return ResponseEntity.of(Optional.of(new PhrasePage()));
    }
    if (document.isEmpty()) return ResponseEntity.of(Optional.of(new PhrasePage()));

    boolean textFilter = !(text == null || text.trim().isEmpty());

    HashSet<Phrase> phraseSet = new HashSet<>(
        phraseService.getPhrasesForDocument(document.get().getId(), false));
    if (textFilter) phraseSet.retainAll(phraseService.getPhraseByText(text, false));
    return ResponseEntity.ok(ApiModelMapper.toPhrasePage(pageFromSet(phraseSet, page)));
  }

  @Override
  public ResponseEntity<Phrase> getPhraseById(String phraseId, List<String> include) {
    return ResponseEntity.of(phraseService.getPhraseById(phraseId));
  }

  private Page<Phrase> pageFromSet(Set<Phrase> phraseSet, Integer page) {
    if (phraseSet.isEmpty()) return Page.empty();
    if (page != null) page -= 1;

    Pageable pageRequest = (page != null && page >= 0) ? PageRequest.of(page, pageSize) : Pageable.unpaged();
    List<Phrase> allPhrases = phraseSet.stream()
        .sorted(Comparator.comparing(Phrase::getId))
        .collect(Collectors.toList());

    int start = (int) pageRequest.getOffset();
    int end = Math.min((start + pageRequest.getPageSize()), phraseSet.size());

    return new PageImpl<>(allPhrases.subList(start, end), pageRequest, allPhrases.size());
  }
}
