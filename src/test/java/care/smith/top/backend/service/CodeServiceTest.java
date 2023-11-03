package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.backend.AbstractTest;
import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystemPage;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CodeServiceTest extends AbstractTest {
  @Autowired OLSCodeService codeService;

  @Test
  void getSuggestions() {
    CodePage suggestions = codeService.getCodeSuggestions(null, "term", Collections.emptyList(), 1);
    assertThat(suggestions).isNotNull().satisfies(s -> assertThat(s.getContent()).isNotEmpty());
  }

  @Test
  void getCodeSystems() {
    CodeSystemPage codeSystems = codeService.getCodeSystems(null, null, null, 1);
    assertThat(codeSystems).isNotNull().satisfies(cs -> assertThat(cs.getContent()).isNotEmpty());
  }
}
