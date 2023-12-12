package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphEntity;
import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import care.smith.top.backend.model.neo4j.DocumentNodeEntity;
import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.repository.elasticsearch.DocumentRepository;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.repository.neo4j.DocumentNodeRepository;
import care.smith.top.backend.repository.neo4j.PhraseNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptCluster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    ConceptGraphEntity conceptGraph = conceptGraphsRepository.getGraphForIdAndProcess(graphId, processName);
    Arrays.stream(conceptGraph.getNodes()).forEach(
            phraseNodeObject -> {
              if (!phraseNodeRepository.phraseNodeExists(phraseNodeObject.getId())) {
                documentSet.addAll(Arrays.stream(phraseNodeObject.getDocuments()).collect(Collectors.toSet()));
                phraseNodeRepository.save(phraseNodeObject.toPhraseNodeEntity());
              }
            }
    );
    documentRepository.findAllById(documentSet).forEach(
            documentEntity -> {
              documentNodeRepository.save(
                      new DocumentNodeEntity(documentEntity.getId(), documentEntity.getDocumentName()));
            }
    );
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
