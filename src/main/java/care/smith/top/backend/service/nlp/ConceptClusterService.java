package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import care.smith.top.backend.model.neo4j.PhraseNodeEntity;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.PipelineResponse;
import care.smith.top.top_document_query.concept_cluster.ConceptPipelineManager;
import care.smith.top.top_document_query.concept_cluster.model.ConceptGraphEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Value("${spring.paging.page-size:10}")
  private int pageSize = 10;
  private final Function<ConceptNodeEntity, ConceptCluster> conceptEntityMapper =
      conceptEntity ->
          new ConceptCluster()
              .id(conceptEntity.conceptId())
              .labels(String.join(", ", conceptEntity.lables()));
  private final ConceptPipelineManager pipelineManager;
  @Autowired private ConceptClusterNodeRepository conceptNodeRepository;
  @Autowired private PhraseNodeRepository phraseNodeRepository;
  @Autowired private DocumentNodeRepository documentNodeRepository;

  public ConceptClusterService(
      @Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri) {
    pipelineManager = new ConceptPipelineManager(conceptGraphsApiUri);
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
  public void evictConceptsFromCache() {}

  @Cacheable(value = "concepts")
  public Page<ConceptCluster> concepts() {
    return conceptsForPage(null);
  }
  public Page<ConceptCluster> conceptsForPage(Integer page) {
    List<ConceptCluster> conceptClusterList = conceptNodeRepository.findAll().stream()
        .map(conceptEntityMapper)
        .collect(Collectors.toList());
    return new PageImpl<>(conceptClusterList, getPageRequestOf(page), conceptClusterList.size());
  }

  public ConceptCluster conceptById(String conceptId) {
    return conceptEntityMapper.apply(
        conceptNodeRepository.findOne(conceptWithId(conceptId)).orElse(null));
  }

  public Page<ConceptCluster> conceptsByDocumentId(String documentId, Integer page) {
    List<ConceptCluster> conceptClusterList = conceptNodeRepository.getConceptNodesByDocumentId(documentId).stream()
        .map(conceptEntityMapper)
        .collect(Collectors.toList());
    return new PageImpl<>(conceptClusterList, getPageRequestOf(page), conceptClusterList.size());
  }

  public Pair<String, Thread> createSpecificGraphsInNeo4j(
      String processName, List<String> graphIds) {
    Map<String, ConceptGraphEntity> graphs =
        pipelineManager.getConceptGraphs(processName, graphIds);
    Thread t = new Thread(() -> graphs.forEach(this::createGraphInNeo4j));
    t.start();
    return new ImmutablePair<>(processName, t);
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

  private void createGraphInNeo4j(String graphId, ConceptGraphEntity conceptGraph) {
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
          new ConceptNodeEntity(graphId, labels, new HashSet<>(phraseNodeEntityMap.values())));
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

    // TODO: Save Document Nodes and by extension relationships 'DOCUMENT--HAS_PHRASE->PHRASE'
    //    documentRepository
    //        .findAllById(documentId2PhraseIdMap.keySet())
    //        .forEach(
    //            documentEntity -> {
    //              documentNodeRepository.save(
    //                  new DocumentNodeEntity(
    //                      documentEntity.getId(),
    //                      documentEntity.getDocumentName(),
    //                      documentId2PhraseIdMap.get(documentEntity.getId()).stream()
    //                          .map(phraseNodeEntityMap::get)
    //                          .collect(Collectors.toSet())));
    //            });
  }

  private Pageable getPageRequestOf(Integer page) {
    if (page == null) return Pageable.unpaged();
    return PageRequest.of(page, pageSize);
  }
}
