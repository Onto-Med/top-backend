package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystemPage;
import java.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author ralph
 */
@SpringBootTest
public class CodeServiceTest extends AbstractTest {
  @Autowired OLSCodeService codeService;

  @Test
  @Disabled
  void getSuggestions() {
    CodePage suggestions =
        codeService.getCodeSuggestions(null, "cancer", Collections.emptyList(), 1);
    assertThat(suggestions).isNotNull().satisfies(s -> assertThat(s.getContent()).isNotEmpty());
  }

  @Test
  @Disabled
  void getCodeSystems() {
    CodeSystemPage codeSystems = codeService.getCodeSystems(null, null, null, 1);
    assertThat(codeSystems).isNotNull().satisfies(cs -> assertThat(cs.getContent()).isNotEmpty());
  }
}
