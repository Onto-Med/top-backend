package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.model.nlp.ConceptNodeEntity;
import care.smith.top.backend.model.nlp.PhraseNodeEntity;
import care.smith.top.model.ConceptCluster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConceptServiceTest extends AbstractNLPTest {

  List<ConceptNodeEntity> conceptList = new ArrayList<>();

  void populateNeo4j(int conceptCount) {
    conceptList.clear();
    conceptClusterNodeRepository.deleteAll();
    phraseRepository.deleteAll();

    for (int i = 0; i < conceptCount; i++) {
      List<String> phraseList =
          List.of(
              String.format("c%s-phrase1", i),
              String.format("c%s-phrase2", i),
              String.format("c%s-phrase3", i));

      List<PhraseNodeEntity> phraseEntityList = new ArrayList<>();
      for (String phrase : phraseList) {
        String[] substrings = phrase.split("-");
        String phraseName = substrings[1];
        PhraseNodeEntity phraseEntity =
            new PhraseNodeEntity(
                List.of(),
                !phraseName.substring("phrase".length()).equals("3"),
                phraseName,
                phraseName.substring("phrase".length()));
        phraseEntityList.add(phraseEntity);
        phraseRepository.save(phraseEntity);
      }

      ConceptNodeEntity concept =
          new ConceptNodeEntity(
              String.format("c%s", i), // id that is retrieved by Concept::getId()
              phraseList, // list of labels that is retrieved by Concept::getLabels()
              new HashSet<>(phraseEntityList));
      conceptList.add(concept);
      conceptClusterNodeRepository.save(concept);
    }
  }

  @Test
  void concepts() {
    int conceptCount = 5;
    populateNeo4j(conceptCount);

    // count the concepts
    assertEquals(conceptCount, conceptService.concepts().size());

    // check if all concepts have the proper labels
    for (ConceptCluster concept : conceptService.concepts()) {
      ConceptNodeEntity conceptNodeEntity =
          conceptList.get(Integer.parseInt(concept.getId().substring(1)));
      assertEquals(String.join(", ", conceptNodeEntity.lables()), concept.getLabels());
    }
  }

  @Test
  void conceptById() {
    int conceptCount = 5;
    populateNeo4j(conceptCount);

    String testConceptId =
        String.format("c%s", ThreadLocalRandom.current().nextInt(0, conceptCount));
    ConceptCluster concept = conceptService.conceptById(testConceptId);
    assertEquals(testConceptId, concept.getId());
  }
}
