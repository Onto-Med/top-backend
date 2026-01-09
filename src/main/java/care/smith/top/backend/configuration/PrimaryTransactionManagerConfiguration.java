package care.smith.top.backend.configuration;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration to resolve the TransactionManager ambiguity caused by having synchronous
 * (JPA/Neo4j) and reactive (WebFlux) dependencies.
 */
@Configuration
public class PrimaryTransactionManagerConfiguration {
  @Primary
  @Bean(name = "jpaTransactionManager")
  PlatformTransactionManager jpaTransactionManager(
      EntityManagerFactory entityManagerFactory, DataSource dataSource) {
    JpaTransactionManager txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(entityManagerFactory);
    txManager.setDataSource(dataSource);
    return txManager;
  }

  @Bean(name = "neo4jTransactionManager")
  Neo4jTransactionManager neo4jTransactionManager(Driver driver) {
    return new Neo4jTransactionManager(driver);
  }
}
