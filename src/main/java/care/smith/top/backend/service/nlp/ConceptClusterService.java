
package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.model.neo4j.PhraseNodeEntity;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.PipelineResponse;
import care.smith.top.model.PipelineResponseStatus;
import care.smith.top.top_document_query.adapter.TextAdapter;
import care.smith.top.top_document_query.concept_cluster.ConceptPipelineManager;
import care.smith.top.top_document_query.concept_cluster.model.ConceptGraphEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterService implements ContentService {
  //ToDo: think about: right now, the filtering of concepts by their corpusId is not made in neo4j
  // but rather in the service -> don't know if this has some performance implications

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;
  private final ConceptPipelineManager pipelineManager;
  private final ConceptClusterNodeRepository conceptNodeRepository;
  private final PhraseNodeRepository phraseNodeRepository;
  private final DocumentNodeRepository documentNodeRepository;
  private final HashMap<String, Thread> conceptClusterProcesses;

  public ConceptClusterService(
      @Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri, ConceptClusterNodeRepository conceptNodeRepository, PhraseNodeRepository phraseNodeRepository, DocumentNodeRepository documentNodeRepository) {
    pipelineManager = new ConceptPipelineManager(conceptGraphsApiUri);
    this.conceptNodeRepository = conceptNodeRepository;
    this.phraseNodeRepository = phraseNodeRepository;
    this.documentNodeRepository = documentNodeRepository;
    this.conceptClusterProcesses = new HashMap<>();
  }

  public HashMap<String, Thread> getConceptClusterProcesses() {
    return conceptClusterProcesses;
  }

  public Thread getConceptClusterProcess(String pipelineId) {
    return this.conceptClusterProcesses.get(pipelineId);
  }

  public Boolean conceptClusterProcessesContainsKey(String pipelineId) {
    return this.conceptClusterProcesses.containsKey(pipelineId);
  }

  public HashMap<String, Thread> addToClusterProcesses(String pipelineId, Thread clusterCreationThread) {
    this.conceptClusterProcesses.put(pipelineId, clusterCreationThread);
    return conceptClusterProcesses;
  }

  public Thread removeFromClusterProcesses(String pipelineId) {
    return this.conceptClusterProcesses.remove(pipelineId);
  }

  public PipelineResponse deleteCompletePipelineAndResults(String pipelineId) {
    try {
      if (this.conceptClusterProcessesContainsKey(pipelineId)) {
        if (this.getConceptClusterProcess(pipelineId).isAlive()) this.getConceptClusterProcess(pipelineId).interrupt();
        this.removeFromClusterProcesses(pipelineId);
      }
      this.evictConceptsFromCache(pipelineId);
      this.removeClustersFromNeo4j(pipelineId);
    } catch (Exception e) {
      return new PipelineResponse()
          .pipelineId(pipelineId)
          .status(PipelineResponseStatus.FAILED)
          .response("Pipeline could not be deleted. -- " + e.getMessage());
    }
    return new PipelineResponse()
        .pipelineId(pipelineId)
        .status(PipelineResponseStatus.SUCCESSFUL)
        .response("Pipeline deleted.");
  }

  static Statement conceptWithId(String id) {
    Node concept =
        Cypher.node("Concept").named("concept").withProperties("conceptId", Cypher.literalOf(id));
    return Cypher.match(concept).returning(concept).build();
  }

  @Override
  public long count() {
    return conceptNodeRepository.count();
  }

  @CacheEvict(value = "concepts", allEntries = true)
  public void evictAllConceptsFromCache() {}

  @CacheEvict(value = "concepts", key = "{#corpusId}")
  public void evictConceptsFromCache(String corpusId) {}

  @Cacheable(value = "concepts")
  public Page<ConceptCluster> concepts() {
    return conceptsForPage(null, null);
  }

  @Cacheable(value = "concepts", key = "{#corpusId}")
  public Page<ConceptCluster> concepts(String corpusId) {
    return conceptsForPage(corpusId, null);
  }

  public Page<ConceptCluster> conceptsForPage(String corpusId, Integer page) {
    List<ConceptCluster> conceptClusterList = conceptNodeRepository.findAll().stream()
        .filter(conceptNodeEntity -> corpusId == null || Objects.equals(conceptNodeEntity.corpusId(), corpusId))
        .sorted(Comparator.comparing(ConceptNodeEntity::conceptId))
        .map(ConceptNodeEntity::toApiModel)
        .collect(Collectors.toList());
    return new PageImpl<>(conceptClusterList, getPageRequestOf(page), conceptClusterList.size());
  }

  public ConceptCluster conceptById(String conceptId, String corpusId) {
    return conceptNodeRepository
        .findOne(conceptWithId(conceptId))
        .filter(conceptNodeEntity -> corpusId == null || Objects.equals(conceptNodeEntity.corpusId(), corpusId))
        .orElse(ConceptNodeEntity.nullConceptNode())
        .toApiModel();
  }

  public Page<ConceptCluster> conceptsByDocumentId(String documentId, String corpusId, Integer page) {
    List<ConceptCluster> conceptClusterList = conceptNodeRepository.getConceptNodesByDocumentId(documentId).stream()
        .filter(conceptNodeEntity -> corpusId == null || Objects.equals(conceptNodeEntity.corpusId(), corpusId))
        .sorted(Comparator.comparing(ConceptNodeEntity::conceptId))
        .map(ConceptNodeEntity::toApiModel)
        .collect(Collectors.toList());
    return new PageImpl<>(conceptClusterList, getPageRequestOf(page), conceptClusterList.size());
  }

  public Page<ConceptCluster> conceptsByLabels(List<String> labels, String corpusId, Integer page) {
    List<ConceptCluster> conceptClusterList = conceptNodeRepository.getConceptNodesByLabels(labels).stream()
        .filter(conceptNodeEntity -> corpusId == null || Objects.equals(conceptNodeEntity.corpusId(), corpusId))
        .sorted(Comparator.comparing(ConceptNodeEntity::conceptId))
        .map(ConceptNodeEntity::toApiModel)
        .collect(Collectors.toList());
    return new PageImpl<>(conceptClusterList, getPageRequestOf(page), conceptClusterList.size());
  }

  public Page<ConceptCluster> conceptsByPhraseIds(List<String> phraseIds, String corpusId, Integer page) {
      List<ConceptCluster> conceptClusterList = conceptNodeRepository.getConceptNodesByPhrases(phraseIds).stream()
          .filter(conceptNodeEntity -> corpusId == null || Objects.equals(conceptNodeEntity.corpusId(), corpusId))
          .sorted(Comparator.comparing(ConceptNodeEntity::conceptId))
          .map(ConceptNodeEntity::toApiModel)
          .collect(Collectors.toList());
      return new PageImpl<>(conceptClusterList, getPageRequestOf(page), conceptClusterList.size());
    }

  public Page<ConceptCluster> conceptsByLabelsAndPhrases(List<String> labels, List<String> phraseIds, String corpusId, Integer page) {
    Set<String> retainIds = conceptNodeRepository.getConceptNodesByLabels(labels).stream()
        .filter(conceptNodeEntity -> corpusId == null || Objects.equals(conceptNodeEntity.corpusId(), corpusId))
        .map(ConceptNodeEntity::conceptId)
        .collect(Collectors.toSet());
    List<ConceptCluster> conceptClusterPhraseList = conceptNodeRepository.getConceptNodesByPhrases(phraseIds).stream()
        .sorted(Comparator.comparing(ConceptNodeEntity::conceptId))
        .filter(conceptNodeEntity -> retainIds.contains(conceptNodeEntity.conceptId()))
        .map(ConceptNodeEntity::toApiModel)
        .collect(Collectors.toList());
    return new PageImpl<>(conceptClusterPhraseList, getPageRequestOf(page), conceptClusterPhraseList.size());
  }


  public List<String> getCorpusIdsInNeo4j() {
    return conceptNodeRepository.getCorpusIds();
  }

  public Pair<String, Thread> createSpecificGraphsInNeo4j(
      String processName, List<String> graphIds, TextAdapter adapter) {
    Map<String, ConceptGraphEntity> graphs =
        pipelineManager.getConceptGraphs(processName, graphIds);
    Thread t = new Thread(() -> graphs.forEach((gId, graph) -> createGraphInNeo4j(gId, processName, graph, adapter)));
    t.start();
    return new ImmutablePair<>(processName, t);
  }

  public void removeClustersFromNeo4j(String pipelineId) {
    conceptNodeRepository.removeAllNodesForCorpusId(pipelineId);
  }

  public void setPipelineResponseStatus(
      PipelineResponse pipelineResponse, String statusStr, String msgStr) {
    String response = "";
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      response =
          mapper.writeValueAsString(
              new Object() {
                private final String status = statusStr;
                private final String message = msgStr;

                public String getMessage() {
                  return message;
                }

                public String getStatus() {
                  return status;
                }
              });
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    pipelineResponse.response(response);
  }

  private void createGraphInNeo4j(String graphId, String processId, ConceptGraphEntity conceptGraph, TextAdapter adapter) {
    Map<String, List<String>> documentId2PhraseIdMap = new HashMap<>();
    Map<String, Integer> phrasesDocumentCount = new HashMap<>();
    Map<String, PhraseNodeEntity> phraseNodeEntityMap = new HashMap<>();

    Arrays.stream(conceptGraph.getNodes())
        .forEach(
            phraseNodeObject -> {
              Arrays.stream(phraseNodeObject.getDocuments())
                  .forEach(
                      documentId -> {
                        if (!documentId2PhraseIdMap.containsKey(documentId)) {
                          documentId2PhraseIdMap.put(
                              documentId, Lists.newArrayList(phraseNodeObject.getId()));
                        } else {
                          documentId2PhraseIdMap.get(documentId).add(phraseNodeObject.getId());
                        }
                      });
              phraseNodeEntityMap.put(
                  phraseNodeObject.getId(),
                  new PhraseNodeEntity(
                      null, false, phraseNodeObject.getLabel(), phraseNodeObject.getId()));
              phrasesDocumentCount.put(
                  phraseNodeObject.getId(), phraseNodeObject.getDocuments().length);
            });

    // Save Concept Nodes (and by extension create relationships 'PHRASE--IN_CONCEPT->CONCEPT' as
    // well as Phrase nodes)
    if (!conceptNodeRepository.conceptNodeExists(graphId)) {
      List<String> labels =
          phrasesDocumentCount.entrySet().stream()
              .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
              .filter(nodeEntry -> phraseNodeEntityMap.containsKey(nodeEntry.getKey()))
              .map(nodeEntry -> phraseNodeEntityMap.get(nodeEntry.getKey()).phraseText())
              .limit(3)
              .collect(Collectors.toList());
      conceptNodeRepository.save(
          new ConceptNodeEntity(graphId, processId, labels, new HashSet<>(phraseNodeEntityMap.values())));
    }

    // Create Relations between Phrases 'PHRASE<-HAS_NEIGHBOR->PHRASE'
    Arrays.stream(conceptGraph.getAdjacency())
        .forEach(
            adjacencyObject -> {
              phraseNodeEntityMap
                  .get(adjacencyObject.getId())
                  .addNeighbors(
                      Arrays.stream(adjacencyObject.getNeighbors())
                          .map(neighbor -> phraseNodeEntityMap.get(neighbor.getId()))
                          .collect(Collectors.toSet()));
              phraseNodeRepository.save(phraseNodeEntityMap.get(adjacencyObject.getId()));
            });

    // Save Document Nodes and by extension relationships 'DOCUMENT--HAS_PHRASE->PHRASE'
    try {
      adapter.getDocumentsByIds(documentId2PhraseIdMap.keySet(),null)
          .forEach(
              documentEntity -> {
                documentNodeRepository.save(
                    new DocumentNodeEntity(
                        documentEntity.getId(),
                        documentEntity.getName(),
                        documentId2PhraseIdMap.get(documentEntity.getId()).stream()
                            .map(phraseNodeEntityMap::get)
                            .collect(Collectors.toSet())));
              });
    } catch (IOException e) {
      //ToDo: !
      throw new RuntimeException(e);
    }
  }

  private Pageable getPageRequestOf(Integer page) {
    if (page == null) return Pageable.unpaged();
    return PageRequest.of(page <= 0 ? 0 : page - 1, pageSize);
  }
}
