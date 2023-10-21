package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystemPage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * @author ralph
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CodeServiceTest extends AbstractTest {

  @Autowired private MutableOLSCodeService codeService;

  private DockerComposeContainer olsContainer;

  private static final Logger LOGGER = Logger.getLogger(CodeServiceTest.class.getName());

  @BeforeAll
  void initializeOLS() throws GitAPIException, IOException {

    Path olsSourcePath = Files.createTempDirectory("top-code-test-ols4");
    olsSourcePath.toFile().deleteOnExit();

    File dockerComposeFile = olsSourcePath.resolve("docker-compose.yml").toFile();
    File testOntologyConfigFile =
        olsSourcePath.resolve("dataload/configs/test-ontology.json").toFile();

    LOGGER.info(String.format("**** Cloning OLS 4 repo to %s", olsSourcePath));

    try (Git git =
        Git.cloneRepository()
            .setURI("https://github.com/EBISPOT/ols4.git")
            .setDirectory(olsSourcePath.toFile())
            .call()) {
      LOGGER.info("**** Copying files...");

      FileUtils.copyInputStreamToFile(
          CodeServiceTest.class.getResourceAsStream("/ols4/docker-compose.yml"), dockerComposeFile);
      FileUtils.copyInputStreamToFile(
          CodeServiceTest.class.getResourceAsStream("/ols4/test.owl"),
          olsSourcePath.resolve("testcases/test.owl").toFile());
      FileUtils.copyInputStreamToFile(
          CodeServiceTest.class.getResourceAsStream("/ols4/test-ontology.json"),
          testOntologyConfigFile);

      olsContainer =
          new DockerComposeContainer(dockerComposeFile)
              .withLocalCompose(true)
              .withOptions("--compatibility")
              .withEnv("OLS4_CONFIG", testOntologyConfigFile.getAbsolutePath())
              .withServices("ols4-dataload", "ols4-solr", "ols4-neo4j", "ols4-backend")
              .withExposedService(
                  "ols4-backend", 8080, Wait.forHttp("/api").forStatusCode(200).forStatusCode(401));

      LOGGER.info("**** Starting OLS4 container...");

      olsContainer.start();

      LOGGER.info("**** OLS4 container started.");

      int olsExternalPort = olsContainer.getServicePort("ols4-backend", 8080);

      LOGGER.info(String.format("OLS4 API port: %d", olsExternalPort));

      String endpoint = String.format("http://localhost:%d/api", olsExternalPort);

      codeService.setEndpoint(endpoint);
    }
  }

  @Test
  void getSuggestions() {
    CodePage suggestions = codeService.getCodeSuggestions(null, "term", Collections.emptyList(), 1);
    assertThat(suggestions).isNotNull().satisfies(s -> assertThat(s.getContent()).isNotEmpty());
  }

  @Test
  void getCodeSystems() {
    CodeSystemPage codeSystems = codeService.getCodeSystems(null, null, null, 1);
    assertThat(codeSystems).isNotNull().satisfies(cs -> assertThat(cs.getContent()).isNotEmpty());
  }
}
