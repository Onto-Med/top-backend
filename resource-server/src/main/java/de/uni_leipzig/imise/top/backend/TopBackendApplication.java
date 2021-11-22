package de.uni_leipzig.imise.top.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class TopBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TopBackendApplication.class, args);
	}

}
