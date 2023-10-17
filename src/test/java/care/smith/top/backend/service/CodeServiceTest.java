package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystemPage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author ralph
 */
@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CodeServiceTest extends AbstractTest {

  @Autowired OLSCodeService codeService;

  @Container
  DockerComposeContainer olsContainer;

  @TempDir
  static Path olsSourcePath;

  @BeforeAll
  void initializeOLS() throws GitAPIException, IOException {

    try (Git git = Git.cloneRepository()
            .setURI("https://github.com/EBISPOT/ols4.git")
            .setDirectory(olsSourcePath.toFile())
            .call()) {}

    File dockerComposeFile = olsSourcePath.resolve("docker-compose.yml").toFile();
    File testOntologyConfigFile = olsSourcePath.resolve("dataload/configs/test-ontology.json").toFile();

    FileUtils.copyInputStreamToFile(CodeServiceTest.class.getResourceAsStream("/ols4/docker-compose.yml"), dockerComposeFile);
    FileUtils.copyInputStreamToFile(CodeServiceTest.class.getResourceAsStream("/ols4/test.owl"), olsSourcePath.resolve("testcases/test.owl").toFile());
    FileUtils.copyInputStreamToFile(CodeServiceTest.class.getResourceAsStream("/ols4/test-ontology.json"), testOntologyConfigFile);

    olsContainer = new DockerComposeContainer(dockerComposeFile, "top-testcontainer-ols4")
            .withEnv("OLS4_CONFIG", testOntologyConfigFile.getAbsolutePath())
            .withStartupTimeout(Duration.ofMinutes(60))
            .withExposedService("ols4-backend", 9000,
                    Wait.forListeningPort());
    olsContainer.start();
  }

  @AfterAll
  void stopOLS() {
    olsContainer.stop();
  }

  @Test
  void getSuggestions() {
    CodePage suggestions =
        codeService.getCodeSuggestions(null, "term", Collections.emptyList(), 1);
    assertThat(suggestions).isNotNull().satisfies(s -> assertThat(s.getContent()).isNotEmpty());
  }

  @Test
  void getCodeSystems() {
    CodeSystemPage codeSystems = codeService.getCodeSystems(null, null, null, 1);
    assertThat(codeSystems).isNotNull().satisfies(cs -> assertThat(cs.getContent()).isNotEmpty());
  }
}
