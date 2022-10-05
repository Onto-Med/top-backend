package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.nlp.PhraseRepository;
import care.smith.top.backend.service.ContentService;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.ListValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConceptService implements ContentService {

    @Autowired PhraseRepository phraseRepository;

    @Override
    @Cacheable("conceptCount")
    public long count() {
        return concepts().size();
    }

    @Cacheable("concepts")
    public List<String> concepts() {
        List<String> concepts = new ArrayList<>();
        ((Optional<ListValue>) phraseRepository.getConceptCollection())
                .ifPresent(strings -> concepts.addAll(
                        strings.asList(Value::asString)
                                .stream()
                                .filter(concept -> !Objects.equals(concept, "Phrase"))
                                .map(concept -> concept.substring("Concept_".length()))
                                .collect(Collectors.toList())
                ));
        return concepts;
    }
}
