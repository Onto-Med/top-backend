package care.smith.top.backend;

import care.smith.top.backend.configuration.InfrastructureConfig;
import care.smith.top.backend.configuration.nlp.DocumentQueryConfigMap;
import care.smith.top.backend.configuration.nlp.DocumentQueryPropertySource;
import care.smith.top.top_document_query.adapter.config.Connection;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
    new SpringApplicationBuilder()
        .sources(TopBackendApplication.class)
        .initializers(
            context ->
              context
                  .getEnvironment()
                  .getPropertySources()
                  .addLast(new DocumentQueryPropertySource(new DocumentQueryConfigMap(context.getEnvironment())))
            )
        .run(args);
  }
}