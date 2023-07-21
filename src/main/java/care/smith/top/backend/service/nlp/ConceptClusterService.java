package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.neo4j.ConceptNodeEntity;
import care.smith.top.backend.repository.neo4j.ConceptClusterNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptCluster;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ConceptClusterService implements ContentService {

  private final ConceptClusterNodeRepository conceptRepository;
  private final Function<ConceptNodeEntity, ConceptCluster> conceptEntityMapper =
      conceptEntity ->
          new ConceptCluster()
              .id(conceptEntity.conceptId())
              .labels(String.join(", ", conceptEntity.lables()));

  @Autowired
  public ConceptClusterService(ConceptClusterNodeRepository conceptRepository) {
    this.conceptRepository = conceptRepository;
  }

  static Statement conceptWithId(String id) {
    Node concept =
        Cypher.node("Concept").named("concept").withProperties("conceptId", Cypher.literalOf(id));
    return Cypher.match(concept).returning(concept).build();
  }

  @Override
  @Cacheable("conceptCount")
  public long count() {
    return concepts().size();
  }

  @Cacheable("concepts")
  public List<ConceptCluster> concepts() {
    return conceptRepository.findAll().stream()
        .map(conceptEntityMapper)
        .collect(Collectors.toList());
  }

  public ConceptCluster conceptById(String conceptId) {
    return conceptEntityMapper.apply(
        conceptRepository.findOne(conceptWithId(conceptId)).orElse(null));
  }
}
