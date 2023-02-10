package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.ConceptEntity;
import care.smith.top.backend.repository.nlp.ConceptRepository;
import care.smith.top.model.Concept;
import org.junit.jupiter.api.*;
import org.springframework.test.context.event.annotation.BeforeTestClass;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConceptServiceTest extends AbstractNLPTest {

    List<ConceptEntity> conceptList = new ArrayList<>();

    void populateNeo4j(int conceptCount) {
        conceptList.clear();
        conceptRepository.deleteAll();

        for (int i = 0; i < conceptCount; i++) {
            ConceptEntity concept = new ConceptEntity(
                    String.format("c%s", i), // id that is retrieved by Concept::getId()
                    List.of(String.format("c%s-phrase1", i),
                            String.format("c%s-phrase2", i),
                            String.format("c%s-phrase3", i)) // list of labels that is retrieved by Concept::getLabels()
            );
            conceptList.add(concept);
            conceptRepository.save(concept);
        }
    }

    @Test
    void concepts() {
        int conceptCount = 5;
        populateNeo4j(conceptCount);

        // count the concepts
        assertEquals(conceptService.concepts().size(), conceptCount);

        // check if all concepts have the proper labels
        for (Concept concept : conceptService.concepts()) {
            ConceptEntity conceptEntity = conceptList.get(Integer.parseInt(concept.getId().substring(1)));
            assertEquals(concept.getLabels(), String.join(", ", conceptEntity.lables()));
        }
    }

    @Test
    void conceptById() {
        int conceptCount = 5;
        populateNeo4j(conceptCount);

        String testConceptId = String.format("c%s", ThreadLocalRandom.current().nextInt(0, conceptCount));
        Concept concept = conceptService.conceptById(testConceptId);
        assertEquals(concept.getId(), testConceptId);
    }
}