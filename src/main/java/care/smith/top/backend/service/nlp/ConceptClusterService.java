package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphEntity;
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
import com.google.common.collect.Lists;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.cache.annotation.CachePut;
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
  }

  @Override
  public long count() {
    return (concepts(false).isEmpty()) ? concepts(true).size() : concepts(false).size();
  }

  @CachePut(value = "concepts", condition = "#recalculateCache")
  public List<ConceptCluster> concepts(Boolean recalculateCache) {
    return conceptNodeRepository.findAll().stream()
        .map(conceptEntityMapper)
        .collect(Collectors.toList());
  }

  public ConceptCluster conceptById(String conceptId) {
    return conceptEntityMapper.apply(
        conceptNodeRepository.findOne(conceptWithId(conceptId)).orElse(null));
  }

  public void createGraphInNeo4j(String graphId, String processName) {
    Map<String, List<String>> documentId2PhraseIdMap = new HashMap<>();
    Map<String, Integer> phrasesDocumentCount = new HashMap<>();
    Map<String, PhraseNodeEntity> phraseNodeEntityMap = new HashMap<>();
    ConceptGraphEntity conceptGraph =
        conceptGraphsRepository.getGraphForIdAndProcess(graphId, processName);

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

  public void createAllGraphsInNeo4j(String processName) {
    Arrays.stream(
            conceptGraphsRepository.getGraphStatisticsForProcess(processName).getConceptGraphs())
        .forEach(
            conceptGraph -> {
              createGraphInNeo4j(conceptGraph.getId(), processName);
            });
  }

  static Statement conceptWithId(String id) {
    Node concept =
        Cypher.node("Concept").named("concept").withProperties("conceptId", Cypher.literalOf(id));
    return Cypher.match(concept).returning(concept).build();
  }
}
