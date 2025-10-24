package care.smith.top.backend.nlp.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.backend.util.AbstractNLPTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PhraseServiceTest extends AbstractNLPTest {
  private final String exampleDatasource = "exampledatasource";

  @Test
  void count() {
    assertThat(phraseService.count()).isEqualTo(Long.valueOf(phrases1_2.size()));
  }

  @Test
  void getPhrasesForConcept() {
    assertThat(phraseService.getPhrasesForConcept("c1", exampleDatasource)).isEqualTo(phrases1);
    assertThat(phraseService.getPhrasesForConcept("c2", exampleDatasource)).isEqualTo(phrases2);
  }

  @Test
  void getAllPhrases() {
    assertThat(Set.copyOf(phraseService.getAllPhrases())).isEqualTo(Set.copyOf(phrases1_2));
  }

  @Test
  void getPhraseById() {
    assertThat(phraseService.getPhraseById("p1", exampleDatasource).orElseThrow())
        .isEqualTo(phrases1.get(0));
    assertThat(phraseService.getPhraseById("p2", exampleDatasource).orElseThrow())
        .isEqualTo(phrases2.get(0));
  }

  @Test
  void getPhraseByText() {
    assertThat(Set.copyOf(phraseService.getPhraseByText("phrase", false)))
        .isEqualTo(Set.copyOf(phrases1_2));
    assertThat(Set.copyOf(phraseService.getPhraseByText("here", false)))
        .isEqualTo(Set.copyOf(phrases1_2));
  }

  @Test
  void getPhraseByExactText() {
    assertThat(phraseService.getPhraseByExactText("one phrase here")).isEqualTo(phrases1);
  }

  @Test
  void getPhrasesForDocument() {
    assertThat(phraseService.getPhrasesForDocument("d1", exampleDatasource, false))
        .isEqualTo(phrases2);
    assertThat(Set.copyOf(phraseService.getPhrasesForDocument("d2", exampleDatasource, false)))
        .isEqualTo(Set.copyOf(phrases1_2));
    assertThat(phraseService.getPhrasesForDocument("d2", exampleDatasource, true))
        .isEqualTo(phrases1);
  }

  @Test
  void getPhrasesByIds() {
    assertThat(Set.copyOf(phraseService.getPhrasesByIds(List.of("p1", "p2"), exampleDatasource)))
        .isEqualTo(Set.copyOf(phrases1_2));
  }
}
