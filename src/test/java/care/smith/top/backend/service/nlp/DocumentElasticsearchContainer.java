package care.smith.top.backend.service.nlp;

import care.smith.top.backend.configuration.TopBackendContextInitializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@ContextConfiguration(initializers = TopBackendContextInitializer.class)
public class DocumentElasticsearchContainer extends ElasticsearchContainer {
  private static final String DOCKER_ELASTIC =
      "docker.elastic.co/elasticsearch/elasticsearch:8.8.1";
  private static final String CLUSTER_NAME = "sample-cluster";
  private static final String DISCOVERY_TYPE = "discovery.type";
  private static final String DISCOVERY_TYPE_SINGLE_NODE = "single-node";
  private static final String XPACK_SECURITY_ENABLED = "xpack.security.enabled";

  @Value("spring.elasticsearch.index.name")
  private static String ELASTIC_SEARCH;

  public DocumentElasticsearchContainer() {
    super(DOCKER_ELASTIC);
    addFixedExposedPort(9008, 9200);
    addEnv(CLUSTER_NAME, ELASTIC_SEARCH);
    addEnv(DISCOVERY_TYPE, DISCOVERY_TYPE_SINGLE_NODE);
    addEnv(XPACK_SECURITY_ENABLED, Boolean.FALSE.toString());
  }
}
