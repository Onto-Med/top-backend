package care.smith.top.backend;

import care.smith.top.backend.configuration.TopBackendContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = TopBackendContextInitializer.class)
class TopBackendApplicationTests {

  @Test
  void contextLoads() {}
}
