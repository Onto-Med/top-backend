package care.smith.top.backend.repository.conceptgraphs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

abstract class ConceptGraphsApi {
  protected WebClient conceptGraphsApi;

  @Autowired
  public void setConceptGraphApiEndpoint(@Value("spring.concept-graph.uri") String conceptGraphApiEndpoint) {
    int size = 16 * 1024 * 1024;
    ExchangeStrategies exchangeStrategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();
    conceptGraphsApi =
        WebClient.builder()
            .baseUrl(conceptGraphApiEndpoint)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(exchangeStrategies)
            .build();
  }

  public enum API_GRAPH_METHODS {
    CREATION("/graph/creation"),
    STATISTICS("/graph/statistics"),
    GRAPH("/graph/");

    private String endpoint;

    API_GRAPH_METHODS(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getEndpoint(int graphId) {
      if (Objects.equals(endpoint, GRAPH.endpoint)) return endpoint + graphId;
      return endpoint;
    }

    public String getEndpoint() {
      if (Objects.equals(endpoint, GRAPH.endpoint)) return endpoint + 0;
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }
  }

  public enum API_PIPELINE_METHODS {
    INITIALIZE("/pipeline");

    private String endpoint;

    API_PIPELINE_METHODS(String endpoint) {
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
