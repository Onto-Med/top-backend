package care.smith.top.backend;

import care.smith.top.backend.configuration.InfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.OffsetDateTime;
import java.util.Optional;

@SpringBootApplication
@Import(InfrastructureConfig.class)
@ComponentScan("care.smith.top.backend")
@EnableCaching
@EnableJpaRepositories
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class TopBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(TopBackendApplication.class, args);
  }
}
