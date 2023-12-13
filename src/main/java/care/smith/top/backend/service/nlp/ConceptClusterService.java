package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphEntity;
import care.smith.top.backend.model.conceptgraphs.PhraseNodeObject;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
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

  public ConceptClusterService(
          ConceptClusterNodeRepository conceptNodeRepository,
          PhraseNodeRepository phraseNodeRepository,
          DocumentNodeRepository documentNodeRepository,
          DocumentRepository documentRepository,
          ConceptGraphsRepository conceptGraphsRepository
  ) {
    this.conceptNodeRepository = conceptNodeRepository;
    this.phraseNodeRepository = phraseNodeRepository;
    this.documentNodeRepository = documentNodeRepository;
    this.documentRepository = documentRepository;
    this.conceptGraphsRepository = conceptGraphsRepository;
  }

  @Override
  @Cacheable("conceptCount")
  public long count() {
    return concepts().size();
  }

  @Cacheable("concepts")
  public List<ConceptCluster> concepts() {
    // ToDo: for late - when generating concept clusters from the frontend (as its intended) this
    // cache needs to be recalculated
    return conceptNodeRepository.findAll().stream()
        .map(conceptEntityMapper)
        .collect(Collectors.toList());
  }

  public ConceptCluster conceptById(String conceptId) {
    return conceptEntityMapper.apply(
        conceptNodeRepository.findOne(conceptWithId(conceptId)).orElse(null));
  }

  public void createGraphInNeo4j(String graphId, String processName) {
    Set<String> documentSet = new HashSet<>();
    Map<PhraseNodeObject, Integer> phrasesDocumentCount = new HashMap<>();
    ConceptGraphEntity conceptGraph = conceptGraphsRepository.getGraphForIdAndProcess(graphId, processName);

    // Save Phrase Nodes
    Arrays.stream(conceptGraph.getNodes()).forEach(
            phraseNodeObject -> {
                Set<String> documents = Arrays.stream(phraseNodeObject.getDocuments()).collect(Collectors.toSet());
                documentSet.addAll(documents);
                phrasesDocumentCount.put(phraseNodeObject, documentSet.size());
              if (!phraseNodeRepository.phraseNodeExists(phraseNodeObject.getId())) {
                phraseNodeRepository.save(phraseNodeObject.toPhraseNodeEntity());
              }
            }
    );

    Set<PhraseNodeEntity> currentPhrases =
        phrasesDocumentCount.keySet().stream()
            .map(PhraseNodeObject::toPhraseNodeEntity)
            .collect(Collectors.toSet());

    // Save Concept Nodes and by extension create relationships 'PHRASE-IN_CONCEPT->CONCEPT'
    if (!conceptNodeRepository.conceptNodeExists(graphId)) {
      List<String> labels = phrasesDocumentCount.entrySet().stream()
          .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
          .map(nodeEntry -> nodeEntry.getKey().getLabel())
          .limit(3)
          .collect(Collectors.toList());
      conceptNodeRepository.save(new ConceptNodeEntity(graphId, labels, currentPhrases));
    }

    // Save Document Nodes
    documentRepository
        .findAllById(documentSet)
        .forEach(
            documentEntity -> {
              if (!documentNodeRepository.documentNodeExists(documentEntity.getId())) {
                documentNodeRepository.save(
                    new DocumentNodeEntity(documentEntity.getId(), documentEntity.getDocumentName(), currentPhrases));
              }
            });
  }

  public void createAllGraphsInNeo4j(String processName) {
    Arrays.stream(conceptGraphsRepository.getGraphStatisticsForProcess(processName).getConceptGraphs()).forEach(
            conceptGraph -> {
              createGraphInNeo4j(conceptGraph.getId(), processName);
            }
    );
  }

  static Statement conceptWithId(String id) {
    Node concept =
            Cypher.node("Concept").named("concept").withProperties("conceptId", Cypher.literalOf(id));
    return Cypher.match(concept).returning(concept).build();
  }
}
