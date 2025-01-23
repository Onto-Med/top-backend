package care.smith.top.backend;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.jpa.CategoryRepository;
import care.smith.top.backend.repository.jpa.ConceptRepository;
import care.smith.top.backend.repository.jpa.EntityRepository;
import care.smith.top.backend.repository.jpa.EntityVersionRepository;
import care.smith.top.backend.repository.jpa.OrganisationMembershipRepository;
import care.smith.top.backend.repository.jpa.OrganisationRepository;
import care.smith.top.backend.repository.jpa.PhenotypeRepository;
import care.smith.top.backend.repository.jpa.RepositoryRepository;
import care.smith.top.backend.repository.jpa.UserRepository;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.UserService;
import care.smith.top.backend.util.ResourceHttpHandler;
import care.smith.top.backend.util.ResourcePathHttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
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
  @Autowired protected SubjectRepository subjectRepository;
  @Autowired protected EncounterRepository encounterRepository;
  @Autowired protected SubjectResourceRepository subjectResourceRepository;

  @BeforeAll
  static void initializeOLS() throws IOException {
    olsServer = HttpServer.create(new InetSocketAddress(9000), 0);
    olsServer.createContext(
        "/api/ontologies", new ResourceHttpHandler("/ols4_fixtures/ontologies.json"));
    olsServer.createContext("/api/select", new ResourceHttpHandler("/ols4_fixtures/select.json"));
    olsServer.createContext(
        "/api/ontologies/test/terms",
        new ResourcePathHttpHandler(
            path -> {
              boolean hierarchicalChildren =
                  path.getFileName().toString().equals("hierarchicalChildren");
              Path realPath = hierarchicalChildren ? path.getParent() : path;
              Path resourceRootPath =
                  Path.of("/ols4_fixtures", hierarchicalChildren ? "terms_hierarchy" : "terms");
              Path relativeUriPath = Path.of("/api/ontologies/test/terms").relativize(realPath);
              return resourceRootPath.resolve(relativeUriPath.toString() + ".json");
            }));
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
    subjectRepository.deleteAll();
    encounterRepository.deleteAll();
    subjectResourceRepository.deleteAll();
  }
}
