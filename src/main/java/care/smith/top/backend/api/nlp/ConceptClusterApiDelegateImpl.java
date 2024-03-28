package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptclusterApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;

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
public class ConceptClusterApiDelegateImpl implements ConceptclusterApiDelegate {
  private final Logger LOGGER = Logger.getLogger(ConceptClusterApiDelegateImpl.class.getName());
  private final HashMap<String, Thread> conceptClusterProcesses = new HashMap<>();
  private final ConceptClusterService conceptClusterService;
  private final DocumentService documentService;

  public ConceptClusterApiDelegateImpl(DocumentService documentService, ConceptClusterService conceptClusterService) {
    this.conceptClusterService = conceptClusterService;
    this.documentService = documentService;
  }

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClusterByDocumentId(
      String documentId, String dataSource, List<String> include, Integer page) {
    Document document;
    try {
      TextAdapter adapter = documentService.getAdapterForDataSource(dataSource);
      document = adapter.getDocumentById(documentId).orElseThrow();
    } catch (InstantiationException e) {
      LOGGER.severe("The text adapter '" + dataSource + "' could not be initialized.");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (IOException e) {
      LOGGER.fine("Text DB Server Instance could not be reached/queried.");
      return ResponseEntity.of(Optional.of(new ConceptClusterPage()));
    } catch (NoSuchElementException e) {
      LOGGER.fine(String.format("Document with id %s couldn't be found on Text DB Server.", documentId));
      return ResponseEntity.of(Optional.of(new ConceptClusterPage()));
    }
    return ResponseEntity.ok(
        ApiModelMapper.toConceptClusterPage(conceptClusterService.conceptsByDocumentId(document.getId(), page)));
  }

  @Override
  public ResponseEntity<ConceptCluster> getConceptClusterById(
      String conceptId, List<String> include) {
    return ResponseEntity.ok(conceptClusterService.conceptById(conceptId));
  }

  @Override
  public ResponseEntity<ConceptClusterPage> getConceptClusters(
      List<String> labelsText,
      List<String> phraseId,
      Boolean recalculateCache,
      Integer page,
      List<String> include) {
    if (Boolean.TRUE.equals(recalculateCache)) conceptClusterService.evictConceptsFromCache();

    boolean labels = !(labelsText == null || labelsText.isEmpty());
    boolean phrases = !(phraseId == null || phraseId.isEmpty());

    Page<ConceptCluster> conceptClusterPage;
    if (labels && phrases) {
      conceptClusterPage = conceptClusterService.conceptsByLabelsAndPhrases(labelsText, phraseId, page);
    } else if (labels) {
      conceptClusterPage = conceptClusterService.conceptsByLabels(labelsText, page);
    } else if (phrases) {
      conceptClusterPage = conceptClusterService.conceptsByPhraseIds(phraseId, page);
    } else {
      conceptClusterPage = conceptClusterService.conceptsForPage(page);
    }
    return ResponseEntity.ok(ApiModelMapper.toConceptClusterPage(conceptClusterPage));
  }

  @Override
  public ResponseEntity<PipelineResponse> createConceptClustersForPipelineId(
      String pipelineId, List<String> graphIds) {
    PipelineResponse response = new PipelineResponse().pipelineId(pipelineId);
    conceptClusterService.evictConceptsFromCache();
    if (!conceptClusterProcesses.containsKey(pipelineId)) {
      conceptClusterProcesses.put(
          pipelineId,
          conceptClusterService.createSpecificGraphsInNeo4j(pipelineId, graphIds).getRight());
      conceptClusterService.setPipelineResponseStatus(
          response, "STARTED", "Started Concept Clusters creation ...");
    } else {
      if (conceptClusterProcesses.get(pipelineId).isAlive()) {
        conceptClusterService.setPipelineResponseStatus(
            response, "RUNNING", "Concept Clusters creation is still running ...");
      } else {
        conceptClusterService.setPipelineResponseStatus(
            response, "FINISHED", "Finished Concept Cluster creation for this process.");
      }
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> deleteConceptClustersForPipelineId(String pipelineId) {
    return ConceptclusterApiDelegate.super.deleteConceptClustersForPipelineId(pipelineId);
  }
}
