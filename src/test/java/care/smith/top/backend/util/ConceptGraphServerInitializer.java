package care.smith.top.backend.util;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.TestSocketUtils;

public class ConceptGraphServerInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>, AfterAllCallback {
  protected static HttpServer conceptGraphsApiService;

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (conceptGraphsApiService != null) conceptGraphsApiService.stop(0);
  }

  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    try {
      conceptGraphsApiService =
          HttpServer.create(new InetSocketAddress(TestSocketUtils.findAvailableTcpPort()), 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    conceptGraphsApiService.createContext(
        "/processes", new ResourceHttpHandler("/concept_graphs_api_fixtures/get_processes.json"));
    conceptGraphsApiService.createContext(
        "/graph/statistics",
        new ResourceHttpHandler("/concept_graphs_api_fixtures/get_statistics.json"));
    conceptGraphsApiService.createContext(
        "/graph/0", new ResourceHttpHandler("/concept_graphs_api_fixtures/get_concept_graph.json"));
    conceptGraphsApiService.start();
  }
}
