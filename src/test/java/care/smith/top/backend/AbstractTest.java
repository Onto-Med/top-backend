package care.smith.top.backend;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.jpa.*;
import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.UserService;
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
  @Autowired protected OrganisationApiDelegateImpl organisationApiDelegate;
  @Autowired protected OrganisationService organisationService;
  @Autowired protected OrganisationRepository organisationRepository;
  @Autowired protected RepositoryService repositoryService;
  @Autowired protected RepositoryRepository repositoryRepository;
  @Autowired protected EntityService entityService;
  @Autowired protected CategoryRepository categoryRepository;
  @Autowired protected ConceptRepository conceptRepository;
  @Autowired protected EntityRepository entityRepository;
  @Autowired protected PhenotypeRepository phenotypeRepository;
  @Autowired protected EntityVersionRepository entityVersionRepository;
  @Autowired protected UserRepository userRepository;
  @Autowired protected UserService userService;
  @Autowired protected OrganisationMembershipRepository organisationMembershipRepository;

  @BeforeAll
  static void initializeOLS() throws IOException {
    olsServer = HttpServer.create(new InetSocketAddress(9000), 0);
    olsServer.createContext(
        "/api/ontologies", new OlsHttpHandler("/ols4_fixtures/ontologies.json"));
    olsServer.createContext("/api/select", new OlsHttpHandler("/ols4_fixtures/select.json"));
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
      try (InputStream resource = AbstractTest.class.getResourceAsStream(resourcePath)) {
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
