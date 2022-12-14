package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.ConceptEntity;
import care.smith.top.backend.repository.nlp.ConceptRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Concept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConceptService implements ContentService {
    //ToDo: maybe it's better to restructure the db so that the Concepts are not a label but rather nodes themselves
    // --> done, check performance

//    @Autowired PhraseRepository phraseRepository;
    @Autowired ConceptRepository conceptRepository;

    @Override
    @Cacheable("conceptCount")
    public long count() {
        return concepts().size();
    }

    @Cacheable("concepts")
    public List<Concept> concepts() {
//        List<Concept> concepts = new ArrayList<>();
//        ((Optional<ListValue>) phraseRepository.getConceptCollection())
//                .ifPresent(strings -> concepts.addAll(
//                        strings.asList(Value::asString)
//                                .stream()
//                                .filter(concept -> !Objects.equals(concept, "Phrase"))
//                                .map(concept -> new Concept().text(concept.substring("Concept_".length())))
//                                .collect(Collectors.toList())
//                ));
        return conceptRepository
                .findAll()
                .stream()
                .map(conceptEntityMapper)
                .collect(Collectors.toList());
    }

    private final Function<ConceptEntity, Concept> conceptEntityMapper = conceptEntity -> new Concept()
            .id(conceptEntity.conceptId())
            .labels(String.join(", ", conceptEntity.lables()));
}
