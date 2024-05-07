package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptclusterApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import care.smith.top.top_document_query.adapter.TextAdapter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterApiDelegateImpl implements ConceptclusterApiDelegate {
  private final Logger LOGGER = Logger.getLogger(ConceptClusterApiDelegateImpl.class.getName());
  private final ConceptClusterService conceptClusterService;
  private final DocumentService documentService;

  public ConceptClusterApiDelegateImpl(
      DocumentService documentService, ConceptClusterService conceptClusterService) {
    this.conceptClusterService = conceptClusterService;
    this.documentService = documentService;
  }

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClusterByDocumentId(
      String documentId, String dataSource, List<String> include, Integer page) {
    Document document;
    try {
      TextAdapter adapter = documentService.getAdapterForDataSource(dataSource);
      document = adapter.getDocumentById(documentId, true).orElseThrow();
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (IOException e) {
      LOGGER.fine("Text DB Server Instance could not be reached/queried.");
      return ResponseEntity.of(Optional.of(new ConceptClusterPage()));
    } catch (NoSuchElementException e) {
      LOGGER.fine(
          String.format("Document with id %s couldn't be found on Text DB Server.", documentId));
      return ResponseEntity.of(Optional.of(new ConceptClusterPage()));
    }
    return ResponseEntity.ok(
        ApiModelMapper.toConceptClusterPage(
            conceptClusterService.conceptsByDocumentId(document.getId(), dataSource, page)));
  }

  @Override
  public ResponseEntity<ConceptCluster> getConceptClusterById(
      String conceptId, String corpusId, List<String> include) {
    return ResponseEntity.ok(conceptClusterService.conceptById(conceptId, corpusId));
  }

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClusters(
      List<String> labelsText,
      List<String> phraseId,
      Boolean recalculateCache,
      String corpusId,
      Integer page,
      List<String> include) {
    if (Boolean.TRUE.equals(recalculateCache))
      conceptClusterService.evictConceptsFromCache(corpusId);

    boolean labels = !(labelsText == null || labelsText.isEmpty());
    boolean phrases = !(phraseId == null || phraseId.isEmpty());

    Page<ConceptCluster> conceptClusterPage;
    if (labels && phrases) {
      conceptClusterPage =
          conceptClusterService.conceptsByLabelsAndPhrases(labelsText, phraseId, corpusId, page);
    } else if (labels) {
      conceptClusterPage = conceptClusterService.conceptsByLabels(labelsText, corpusId, page);
    } else if (phrases) {
      conceptClusterPage = conceptClusterService.conceptsByPhraseIds(phraseId, corpusId, page);
    } else {
      conceptClusterPage = conceptClusterService.conceptsForPage(corpusId, page);
    }
    return ResponseEntity.ok(ApiModelMapper.toConceptClusterPage(conceptClusterPage));
  }

  @Override
  public ResponseEntity<List<String>> getCorpusIds() {
    return ResponseEntity.of(Optional.ofNullable(conceptClusterService.getCorpusIdsInNeo4j()));
  }

  @Override
  public ResponseEntity<PipelineResponse> getConceptClusterProcess(String pipelineId) {
    PipelineResponse response = new PipelineResponse().pipelineId(pipelineId);
    if (pipelineId == null || !conceptClusterService.conceptClusterProcessesContainsKey(pipelineId))
      return ResponseEntity.of(
          Optional.of(
              response.response(
                  String.format("Process '%s' not found or no pipelineId provided.", pipelineId))));
    return ResponseEntity.ok(checkConceptClusterProcess(pipelineId, response));
  }

  @Override
  public ResponseEntity<PipelineResponse> createConceptClustersForPipelineId(
      String pipelineId, List<String> graphIds) {
    PipelineResponse response = new PipelineResponse().pipelineId(pipelineId);
    TextAdapter adapter;

    try {
      adapter = documentService.getAdapterForDataSource(pipelineId);
    } catch (InstantiationException e) {
      String message = "No text adapter for '" + pipelineId + "' could be initialized.";
      LOGGER.severe(message);
      response.status(PipelineResponseStatus.FAILED).response(message);
      return ResponseEntity.of(Optional.of(response));
    }

    if (!conceptClusterService.conceptClusterProcessesContainsKey(pipelineId)) {
      conceptClusterService.addToClusterProcesses(
          pipelineId,
          conceptClusterService
              .createSpecificGraphsInNeo4j(pipelineId, graphIds, adapter)
              .getRight());
      response
          .status(PipelineResponseStatus.RUNNING)
          .response("Started Concept Clusters creation ...");
    } else {
      checkConceptClusterProcess(pipelineId, response);
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> deleteConceptClustersForPipelineId(String pipelineId) {
    // ToDo: another response than void?
    PipelineResponse response = conceptClusterService.deleteCompletePipelineAndResults(pipelineId);
    if (response.getStatus().equals(PipelineResponseStatus.SUCCESSFUL))
      return ResponseEntity.ok().build();
    return ResponseEntity.internalServerError().build();
  }

  private PipelineResponse checkConceptClusterProcess(
      String pipelineId, PipelineResponse response) {
    if (conceptClusterService.getConceptClusterProcess(pipelineId).isAlive()) {
      response
          .status(PipelineResponseStatus.RUNNING)
          .response("Concept Clusters creation is still running ...");
    } else {
      response
          .status(PipelineResponseStatus.SUCCESSFUL)
          .response("Finished Concept Cluster creation for this process.");
    }
    return response;
  }
}
