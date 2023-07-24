package care.smith.top.backend;

import care.smith.top.backend.configuration.InfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication(
    exclude = {
      ReactiveElasticsearchRepositoriesAutoConfiguration.class,
      Neo4jReactiveRepositoriesAutoConfiguration.class
    })
@Import(InfrastructureConfig.class)
@ComponentScan("care.smith.top.backend")
@EnableCaching
@EnableJpaRepositories(basePackages = "care.smith.top.backend.repository.jpa")
@EnableNeo4jRepositories(basePackages = "care.smith.top.backend.repository.neo4j")
@EnableElasticsearchRepositories(basePackages = "care.smith.top.backend.repository.elasticsearch")
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class TopBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(TopBackendApplication.class, args);
  }
}
