package care.smith.top.backend.service;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.jpa.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class AbstractTest {
  static HttpServer olsServer;
  @Autowired OrganisationApiDelegateImpl organisationApiDelegate;
  @Autowired OrganisationService organisationService;
  @Autowired OrganisationRepository organisationRepository;
  @Autowired RepositoryService repositoryService;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired EntityService entityService;
  @Autowired CategoryRepository categoryRepository;
  @Autowired ConceptRepository conceptRepository;
  @Autowired EntityRepository entityRepository;
  @Autowired PhenotypeRepository phenotypeRepository;
  @Autowired EntityVersionRepository entityVersionRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Autowired OrganisationMembershipRepository organisationMembershipRepository;

  @BeforeAll
  static void initializeOLS() throws IOException {
    olsServer = HttpServer.create(new InetSocketAddress(9000), 0);
    olsServer.createContext(
        "/api/ontologies", new CodeServiceTest.OlsHttpHandler("/ols4_fixtures/ontologies.json"));
    olsServer.createContext(
        "/api/select", new CodeServiceTest.OlsHttpHandler("/ols4_fixtures/select.json"));
    olsServer.start();
  }

  @AfterAll
  static void stopOLS() {
    olsServer.stop(0);
  }

  @BeforeEach
  public void resetState() {
    organisationRepository.deleteAll();
    userRepository.deleteAll();
  }

  static class OlsHttpHandler implements HttpHandler {
    private final String resourcePath;

    public OlsHttpHandler(String resourcePath) {
      this.resourcePath = resourcePath;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try (InputStream resource = CodeServiceTest.class.getResourceAsStream(resourcePath)) {
        assert resource != null;
        byte[] response = resource.readAllBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
        exchange.getResponseBody().write(response);
      }
      exchange.close();
    }
  }
}
