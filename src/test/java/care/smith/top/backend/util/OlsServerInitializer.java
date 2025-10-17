package care.smith.top.backend.util;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.TestSocketUtils;

public class OlsServerInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>, AfterAllCallback {
  static HttpServer olsServer;

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (olsServer != null) olsServer.stop(0);
  }

  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    try {
      olsServer =
          HttpServer.create(new InetSocketAddress(TestSocketUtils.findAvailableTcpPort()), 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
              return resourceRootPath.resolve(relativeUriPath + ".json");
            }));
    olsServer.start();

    TestPropertyValues.of("coding.terminology-service=http://localhost:" + olsServer.getAddress().getPort() + "/api")
        .applyTo(applicationContext.getEnvironment());
  }
}
