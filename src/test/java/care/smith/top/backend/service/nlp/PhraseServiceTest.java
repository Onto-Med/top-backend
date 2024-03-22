package care.smith.top.backend.service.nlp;

import care.smith.top.backend.AbstractNLPTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class PhraseServiceTest extends AbstractNLPTest {

  @Autowired PhraseService phraseService;

  @Test
  void count() {
    assertThat(phraseService.count()).isEqualTo(Long.valueOf(phrases1_2.size()));
  }

  @Test
  void getPhrasesForConcept() {
    assertThat(phraseService.getPhrasesForConcept("c1")).isEqualTo(phrases1);
    assertThat(phraseService.getPhrasesForConcept("c2")).isEqualTo(phrases2);
  }

  @Test
  void getAllPhrases() {
    assertThat(Set.copyOf(phraseService.getAllPhrases())).isEqualTo(Set.copyOf(phrases1_2));
  }

  @Test
  void getPhraseById() {
    assertThat(phraseService.getPhraseById("p1").orElseThrow()).isEqualTo(phrases1.get(0));
    assertThat(phraseService.getPhraseById("p2").orElseThrow()).isEqualTo(phrases2.get(0));
  }

  @Test
  void getPhraseByText() {
    assertThat(Set.copyOf(phraseService.getPhraseByText("phrase", false))).isEqualTo(Set.copyOf(phrases1_2));
    assertThat(Set.copyOf(phraseService.getPhraseByText("here", false))).isEqualTo(Set.copyOf(phrases1_2));
  }

  @Test
  void getPhraseByExactText() {
    assertThat(phraseService.getPhraseByExactText("one phrase here")).isEqualTo(phrases1);
  }

  @Test
  void getPhrasesForDocument() {
    assertThat(phraseService.getPhrasesForDocument("d1", false)).isEqualTo(phrases2);
    assertThat(Set.copyOf(phraseService.getPhrasesForDocument("d2", false))).isEqualTo(Set.copyOf(phrases1_2));
    assertThat(phraseService.getPhrasesForDocument("d2", true)).isEqualTo(phrases1);
  }
}