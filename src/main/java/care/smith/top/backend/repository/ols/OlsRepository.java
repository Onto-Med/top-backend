package care.smith.top.backend.repository.ols;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

public abstract class OlsRepository {
  protected WebClient terminologyService;

  @Autowired
  public void setTerminologyServiceEndpoint(
      @Value("${coding.terminology-service}") String terminologyServiceEndpoint) {
    int size = 16 * 1024 * 1024;
    ExchangeStrategies exchangeStrategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();
    terminologyService =
        WebClient.builder()
            .baseUrl(terminologyServiceEndpoint)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(exchangeStrategies)
            .clientConnector(
                new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
            .build();
  }

  public enum SEARCH_METHOD {
    SEARCH("/search"),
    SUGGEST("/select");

    private String endpoint;

    SEARCH_METHOD(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }
  }
}
