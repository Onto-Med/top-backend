package care.smith.top.backend.nlp.api;

import care.smith.top.backend.api.nlp.PhraseApiDelegateImpl;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.backend.util.AbstractNLPTest;
import java.io.IOException;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PhraseApiDelegateImplTest extends AbstractNLPTest {
  private final String exampleDatasource = "exampledatasource";
  private PhraseApiDelegateImpl phraseApi;
  private PhraseService phraseService;

  @BeforeEach
  void setUp() throws IOException, InstantiationException {
    phraseService = new PhraseService(phraseRepository, relationRepository);
    phraseApi = new PhraseApiDelegateImpl(phraseService, mockedDocumentService());
  }

  @Test
  void getPhrases() {
    Assertions.assertEquals(
        phrases1,
        Objects.requireNonNull(phraseApi.getPhrases(null, exampleDatasource, "c1", 0).getBody())
            .getContent());

    Assertions.assertEquals(
        phrases2,
        Objects.requireNonNull(phraseApi.getPhrases(null, exampleDatasource, "c2", 0).getBody())
            .getContent());
  }

  @Test
  void getPhrasesByDocumentId() {
    Assertions.assertEquals(
        phrases2,
        Objects.requireNonNull(
                phraseApi.getPhrasesByDocumentId("d1", exampleDatasource, null, null, 0).getBody())
            .getContent());
    Assertions.assertEquals(
        phrases1_2,
        Objects.requireNonNull(
                phraseApi.getPhrasesByDocumentId("d2", exampleDatasource, null, null, 0).getBody())
            .getContent());
  }

  @Test
  void getPhraseById() {
    Assertions.assertEquals(
        phrases1.get(0), phraseApi.getPhraseById("p1", exampleDatasource, null).getBody());
    Assertions.assertEquals(
        phrases2.get(0), phraseApi.getPhraseById("p2", exampleDatasource, null).getBody());
  }
}
