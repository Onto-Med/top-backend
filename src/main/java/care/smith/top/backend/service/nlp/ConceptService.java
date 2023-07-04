package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.ConceptNodeEntity;
import care.smith.top.backend.repository.nlp.ConceptNodeRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptCluster;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConceptService implements ContentService {

    private final ConceptNodeRepository conceptRepository;

    @Autowired
    public ConceptService(ConceptNodeRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Cacheable("conceptCount")
    public long count() {
        return concepts().size();
    }

    @Cacheable("concepts")
    public List<ConceptCluster> concepts() {
        return conceptRepository
                .findAll()
                .stream()
                .map(conceptEntityMapper)
                .collect(Collectors.toList());
    }

    public ConceptCluster conceptById(String conceptId) {
        return conceptEntityMapper.apply(
                conceptRepository
                        .findOne(conceptWithId(conceptId))
                        .orElse(null)
        );
    }

    private final Function<ConceptNodeEntity, ConceptCluster> conceptEntityMapper = conceptEntity -> new ConceptCluster()
            .id(conceptEntity.conceptId())
            .labels(String.join(", ", conceptEntity.lables()));

    static Statement conceptWithId(String id) {
        Node concept = Cypher.node("Concept").named("concept")
                .withProperties("conceptId", Cypher.literalOf(id));
        return Cypher.match(concept).returning(concept).build();
    }

}
