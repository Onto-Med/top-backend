package care.smith.top.backend.util;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgreSqlTestcontainersInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>, AfterAllCallback {
  PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (postgres != null) postgres.stop();
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    postgres.start();

    TestPropertyValues.of(
            "spring.datasource.url=" + postgres.getJdbcUrl(),
            "spring.datasource.username=" + postgres.getUsername(),
            "spring.datasource.password=" + postgres.getPassword())
        .applyTo(applicationContext.getEnvironment());
  }
}
