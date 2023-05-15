package care.smith.top.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * @author ralph
 */
@SpringBootTest
public class CodeServiceTest extends AbstractTest {
  @Autowired OLSCodeService codeService;

  @Test
  void getSuggestions() {
    var suggestions = codeService.getCodeSuggestions(null, "cancer", Collections.emptyList(), 0);
    assertThat(suggestions).isNotNull().isNotEmpty();
  }
}
