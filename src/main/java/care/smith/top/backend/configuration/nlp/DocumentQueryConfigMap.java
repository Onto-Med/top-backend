package care.smith.top.backend.configuration.nlp;

import care.smith.top.top_document_query.adapter.config.Connection;
import care.smith.top.top_document_query.adapter.config.GraphDBConfig;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DocumentQueryConfigMap {
  private final Logger LOGGER = Logger.getLogger(DocumentQueryConfigMap.class.getName());
  private Map<String, Object> configMap;
  private String name;

  public Map<String, Object> getConfigMap() {
    return configMap;
  }

  public void setConfigMap(Map<String, Object> configMap) {
    this.configMap = configMap;
  }

  public void setConfigMap(TextAdapterConfig config, Environment environment) {
    this.configMap = new HashMap<>() {{
      put("spring.elasticsearch.uris", config.getConnection().getUrl() + ":" + config.getConnection().getPort());
      put("spring.elasticsearch.index.name", Arrays.stream(config.getIndex()).findFirst().orElse("documents")); //ToDo: look into this - provide list here possible, too?
      put("spring.neo4j.uri", config.getGraphDB().getConnection().getUrl() + ":" + config.getGraphDB().getConnection().getPort());
      put("spring.neo4j.authentication.username",
          (config.getGraphDB().getAuthentication() == null || config.getGraphDB().getAuthentication().getUsername() == null) ?
              environment.getProperty("top.documents.security.neo4j.username") : config.getGraphDB().getAuthentication().getUsername()
      );
      put("spring.neo4j.authentication.password",
          (config.getGraphDB().getAuthentication() == null || config.getGraphDB().getAuthentication().getPassword() == null) ?
              environment.getProperty("top.documents.security.neo4j.password") : config.getGraphDB().getAuthentication().getPassword()
      );
    }};
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DocumentQueryConfigMap(Environment environment) {
    TextAdapterConfig config = getFirstTextAdapterConfig(environment.getProperty("top.documents.data-source-config-dir"));
    if (config == null) config = defaultConfig();
    this.setName(config.getId());
    this.setConfigMap(config, environment);
  }

  private TextAdapterConfig getFirstTextAdapterConfig(String dataSourceConfigDir) {
    try (Stream<Path> paths =
             Files.list(Path.of(dataSourceConfigDir)).filter(f -> !Files.isDirectory(f))) {
      return paths
          .map(this::toTextAdapterConfig)
          .filter(Objects::nonNull)
          .min(Comparator.comparing(TextAdapterConfig::getId))
          .orElseThrow();
    } catch (Exception e) {
      LOGGER.warning(
          String.format(
              "Could not load text adapter configs from dir '%s'. Using default adapter settings.", dataSourceConfigDir));
      return null;
    }
  }

  private TextAdapterConfig toTextAdapterConfig(Path path) {
    try {
      TextAdapterConfig textAdapterConfig = TextAdapterConfig.getInstance(path.toString());
      return textAdapterConfig.getId() == null ? null : textAdapterConfig;
    } catch (Exception e) {
      LOGGER.warning(
          String.format(
              "Text adapter config could not be loaded from file '%s'. Error: %s",
              path.toString(), e.getMessage()));
    }
    return null;
  }

  private TextAdapterConfig defaultConfig() {
    TextAdapterConfig config = new TextAdapterConfig();
    config.setId("Default Adapter");

    Connection elastic_connection = new Connection();
    elastic_connection.setUrl("http://localhost");
    elastic_connection.setPort("9200");

    config.setConnection(elastic_connection);
    config.setIndex(new String[]{"documents"});
    config.setField(new String[]{"text"});

    Connection neo4j_connection = new Connection();
    neo4j_connection.setUrl("bolt://localhost");
    neo4j_connection.setPort("7687");

    GraphDBConfig neo4j = new GraphDBConfig();
    neo4j.setConnection(neo4j_connection);

    return config;
  }
}

