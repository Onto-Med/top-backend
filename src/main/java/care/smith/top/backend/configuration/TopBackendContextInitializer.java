package care.smith.top.backend.configuration;

import care.smith.top.backend.configuration.nlp.DocumentQueryConfigMap;
import care.smith.top.backend.configuration.nlp.DocumentQueryPropertySource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class TopBackendContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    applicationContext
        .getEnvironment()
        .getPropertySources()
        .addLast(
            new DocumentQueryPropertySource(
                new DocumentQueryConfigMap(applicationContext.getEnvironment())));
  }
}
