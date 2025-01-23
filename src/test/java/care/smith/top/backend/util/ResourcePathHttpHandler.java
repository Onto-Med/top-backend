package care.smith.top.backend.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;

public class ResourcePathHttpHandler implements HttpHandler {

  @FunctionalInterface
  public interface PathMapper {
    Path map(Path path);
  }

  private final PathMapper mapper;

  public ResourcePathHttpHandler(PathMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Path resourcePath = mapper.map(Path.of(exchange.getRequestURI().getRawPath()));
    try (InputStream resource =
        ResourcePathHttpHandler.class.getResourceAsStream(resourcePath.toString())) {
      assert resource != null;
      byte[] response = resource.readAllBytes();
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
      exchange.getResponseBody().write(response);
    }
    exchange.close();
  }
}
