package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.ConceptEntity;
import care.smith.top.backend.repository.nlp.ConceptRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Concept;
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

    private final ConceptRepository conceptRepository;

    @Autowired
    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Cacheable("conceptCount")
    public long count() {
        return concepts().size();
    }

    @Cacheable("concepts")
    public List<Concept> concepts() {
        return conceptRepository
                .findAll()
                .stream()
                .map(conceptEntityMapper)
                .collect(Collectors.toList());
    }

    public Concept conceptById(String conceptId) {
        return conceptEntityMapper.apply(
                conceptRepository
                        .findOne(conceptWithId(conceptId))
                        .orElse(null)
        );
    }

    private final Function<ConceptEntity, Concept> conceptEntityMapper = conceptEntity -> new Concept()
            .id(conceptEntity.conceptId())
            .labels(String.join(", ", conceptEntity.lables()));

    static Statement conceptWithId(String id) {
        Node concept = Cypher.node("Concept").named("concept")
                .withProperties("conceptId", Cypher.literalOf(id));
        return Cypher.match(concept).returning(concept).build();
    }
}
