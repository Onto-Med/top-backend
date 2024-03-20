package care.smith.top.backend.api.nlp;

import care.smith.top.backend.AbstractNLPTest;
import care.smith.top.backend.service.nlp.PhraseService;
import care.smith.top.model.Phrase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PhraseApiDelegateImplTest extends AbstractNLPTest {
  private DocumentApiDelegateImpl documentApi;
  private PhraseApiDelegateImpl phraseApi;
  private PhraseService phraseService;

  List<Phrase> phrases1 = List.of(new Phrase().id("p1").text("one phrase here").exemplar(true));
  List<Phrase> phrases2 = List.of(new Phrase().id("p2").text("another phrase there").exemplar(false));
  List<Phrase> phrases1_2 = List.of(phrases1.get(0), phrases2.get(0));

  @BeforeEach
  void setUp() throws IOException, InstantiationException {
    phraseService = new PhraseService(phraseRepository);
    documentApi = new DocumentApiDelegateImpl(mockedDocumentService());
    phraseApi = new PhraseApiDelegateImpl(phraseService, mockedDocumentService());
  }

  @Test
  void getPhrases() {
    Assertions.assertEquals(
        phrases1,
        Objects.requireNonNull(phraseApi.getPhrases(null, "c1", 0).getBody()).getContent());

    Assertions.assertEquals(
        phrases2,
        Objects.requireNonNull(phraseApi.getPhrases(null, "c2", 0).getBody()).getContent());
  }

  @Test
  void getPhrasesByDocumentId() {
    Assertions.assertEquals(
        phrases2,
        Objects.requireNonNull(phraseApi.getPhrasesByDocumentId(
            "d1", "exampleDataSource", null, null, 0).getBody()).getContent());
    Assertions.assertEquals(
        phrases1_2,
        Objects.requireNonNull(phraseApi.getPhrasesByDocumentId(
            "d2", "exampleDataSource", null, null, 0).getBody()).getContent());
  }

  @Test
  void getPhraseById() {
    Assertions.assertEquals(phrases1.get(0), phraseApi.getPhraseById("p1", null).getBody());
    Assertions.assertEquals(phrases2.get(0), phraseApi.getPhraseById("p2", null).getBody());
  }
}