package care.smith.top.backend.configuration;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration to resolve the TransactionManager ambiguity caused by having both synchronous
 * (JPA/JDBC) and reactive (WebFlux) dependencies.
 *
 * <p>This manually defines the synchronous JpaTransactionManager and marks it as the default choice
 * (@Primary).
 */
@Configuration
public class PrimaryTransactionManagerConfiguration {
  @Primary
  @Bean(name = "transactionManager")
  public PlatformTransactionManager transactionManager(
      EntityManagerFactory entityManagerFactory, DataSource dataSource) {

    JpaTransactionManager txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(entityManagerFactory);
    txManager.setDataSource(dataSource);

    return txManager;
  }
}
