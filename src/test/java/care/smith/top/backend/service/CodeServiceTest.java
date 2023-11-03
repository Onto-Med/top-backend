package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystemPage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CodeServiceTest extends AbstractTest {
  static HttpServer olsServer;
  @Autowired OLSCodeService codeService;

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
