package care.smith.top.backend;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.jpa.*;
import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.UserService;
import care.smith.top.backend.util.ResourceHttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration()
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
        "/api/ontologies", new ResourceHttpHandler("/ols4_fixtures/ontologies.json"));
    olsServer.createContext("/api/select", new ResourceHttpHandler("/ols4_fixtures/select.json"));
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
}
