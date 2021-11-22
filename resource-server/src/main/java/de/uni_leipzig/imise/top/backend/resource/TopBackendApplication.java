package de.uni_leipzig.imise.top.backend.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan("de.uni_leipzig.imise.top.backend")
@EnableTransactionManagement
public class TopBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(TopBackendApplication.class, args);
  }
}
