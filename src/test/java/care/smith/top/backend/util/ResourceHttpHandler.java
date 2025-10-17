package care.smith.top.backend.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class ResourceHttpHandler implements HttpHandler {
  private final String resourcePath;

  public ResourceHttpHandler(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try (InputStream resource = AbstractJpaTest.class.getResourceAsStream(resourcePath)) {
      assert resource != null;
      byte[] response = resource.readAllBytes();
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
      exchange.getResponseBody().write(response);
    }
    exchange.close();
  }
}
