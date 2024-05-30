package care.smith.top.backend;

import care.smith.top.backend.configuration.InfrastructureConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication(exclude = {Neo4jReactiveRepositoriesAutoConfiguration.class})
@Import(InfrastructureConfig.class)
@ComponentScan("care.smith.top.backend")
@EnableCaching
@EnableJpaRepositories(basePackages = "care.smith.top.backend.repository.jpa")
@EnableNeo4jRepositories(basePackages = "care.smith.top.backend.repository.neo4j")
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class TopBackendApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder().sources(TopBackendApplication.class).run(args);
  }
}
