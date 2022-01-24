package care.smith.top.backend.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@ComponentScan("care.smith.top.backend")
@EnableNeo4jRepositories
@EnableNeo4jAuditing
public class TopBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(TopBackendApplication.class, args);
  }
}
