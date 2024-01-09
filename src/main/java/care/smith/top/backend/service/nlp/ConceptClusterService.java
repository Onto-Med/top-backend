package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphEntity;
import care.smith.top.backend.model.conceptgraphs.GraphStatsEntity;
import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.model.neo4j.PhraseNodeEntity;
import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.repository.elasticsearch.DocumentRepository;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptCluster;
import care.smith.top.model.PipelineResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterService implements ContentService {

  private final ConceptClusterNodeRepository conceptNodeRepository;
  private final PhraseNodeRepository phraseNodeRepository;
  private final DocumentNodeRepository documentNodeRepository;
  private final DocumentRepository documentRepository;
  private final ConceptGraphsRepository conceptGraphsRepository;
  private final Function<ConceptNodeEntity, ConceptCluster> conceptEntityMapper =
      conceptEntity ->
          new ConceptCluster()
              .id(conceptEntity.conceptId())
              .labels(String.join(", ", conceptEntity.lables()));
  private long conceptCount;

  public ConceptClusterService(
      ConceptClusterNodeRepository conceptNodeRepository,
      PhraseNodeRepository phraseNodeRepository,
      DocumentNodeRepository documentNodeRepository,
      DocumentRepository documentRepository,
      ConceptGraphsRepository conceptGraphsRepository) {
    this.conceptNodeRepository = conceptNodeRepository;
    this.phraseNodeRepository = phraseNodeRepository;
    this.documentNodeRepository = documentNodeRepository;
    this.documentRepository = documentRepository;
    this.conceptGraphsRepository = conceptGraphsRepository;
    this.conceptCount = 0;
  }

  @Override
  public long count() {
    return conceptCount;
  }

  @CacheEvict(value = "concepts", allEntries = true)
  public void evictConceptsFromCache() {}

  @Cacheable(value = "concepts")
  public List<ConceptCluster> concepts() {
    List<ConceptCluster> conceptClusterList =  conceptNodeRepository.findAll().stream()
        .map(conceptEntityMapper)
        .collect(Collectors.toList());
    this.conceptCount = conceptClusterList.size();
    return conceptClusterList;
  }

  public ConceptCluster conceptById(String conceptId) {
    return conceptEntityMapper.apply(
        conceptNodeRepository.findOne(conceptWithId(conceptId)).orElse(null));
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
                  phraseNodeObject.getId(), phraseNodeObject.toPhraseNodeEntity());
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

    // Save Document Nodes and by extension relationships 'DOCUMENT--HAS_PHRASE->PHRASE'
    documentRepository
        .findAllById(documentId2PhraseIdMap.keySet())
        .forEach(
            documentEntity -> {
              documentNodeRepository.save(
                  new DocumentNodeEntity(
                      documentEntity.getId(),
                      documentEntity.getDocumentName(),
                      documentId2PhraseIdMap.get(documentEntity.getId()).stream()
                          .map(phraseNodeEntityMap::get)
                          .collect(Collectors.toSet())));
            });
  }

  public Pair<String, Thread> createSpecificGraphsInNeo4j(String processName, List<String> graphIds) {
    HashMap<String, ConceptGraphEntity> entities = new HashMap<>();
    Stream<GraphStatsEntity> conceptGraphStream;

    if (graphIds == null || graphIds.isEmpty()) {
      Arrays.stream(conceptGraphsRepository.getGraphStatisticsForProcess(processName).getConceptGraphs())
          .forEach(conceptGraph -> entities.put(
              conceptGraph.getId(), conceptGraphsRepository.getGraphForIdAndProcess(conceptGraph.getId(), processName))
          );
      conceptGraphStream = Arrays.stream(
          conceptGraphsRepository.getGraphStatisticsForProcess(processName).getConceptGraphs()
      );
    } else {
      graphIds.forEach(graphId -> entities.put(
          graphId, conceptGraphsRepository.getGraphForIdAndProcess(graphId, processName))
      );
      conceptGraphStream = Arrays.stream(
              conceptGraphsRepository.getGraphStatisticsForProcess(processName).getConceptGraphs()
          ).filter(conceptGraph -> graphIds.contains(conceptGraph.getId()));
    }
    Thread t = new Thread(() -> conceptGraphStream
        .forEach(conceptGraph -> {
          // ToDo: threading of single graph creation seems to be not working; throws sometimes 'NoSuchRecordException'
//          Thread t = new Thread(() -> createGraphInNeo4j(conceptGraph.getId(), entities.get(conceptGraph.getId())));
//          t.start();
          createGraphInNeo4j(conceptGraph.getId(), entities.get(conceptGraph.getId()));
        })
    );
    t.start();
    return new ImmutablePair<>(processName, t);
  }

  static Statement conceptWithId(String id) {
    Node concept =
        Cypher.node("Concept").named("concept").withProperties("conceptId", Cypher.literalOf(id));
    return Cypher.match(concept).returning(concept).build();
  }

  public void setPipelineResponseStatus(PipelineResponse pipelineResponse, String statusStr, String msgStr) {
    String response = "";
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      response = mapper.writeValueAsString(new Object() {
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
}
