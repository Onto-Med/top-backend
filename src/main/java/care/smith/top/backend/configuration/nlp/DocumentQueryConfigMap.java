package care.smith.top.backend.configuration.nlp;

import care.smith.top.top_document_query.adapter.config.ConceptGraphConfig;
import care.smith.top.top_document_query.adapter.config.Connection;
import care.smith.top.top_document_query.adapter.config.GraphDBConfig;
import care.smith.top.top_document_query.adapter.config.TextAdapterConfig;
import org.springframework.core.env.Environment;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DocumentQueryConfigMap {
  private static final String DEFAULT_ES_URL = "http://localhost";
  private static final String DEFAULT_ES_PORT = "9200";
  private static final String DEFAULT_ES_INDEX = "documents";
  private static final String DEFAULT_ES_FIELD = "text";
  private static final String DEFAULT_NEO4J_URL = "bolt://localhost";
  private static final String DEFAULT_NEO4J_PORT = "7687";
  private static final String DEFAULT_CONCEPT_GRAPH_URL = "http://localhost";
  private static final String DEFAULT_CONCEPT_GRAPH_PORT = "9007";
  private static final String SPRING_ES_URI_PROP = "spring.elasticsearch.uris";
  private static final String SPRING_ES_USERNAME_PROP = "spring.elasticsearch.username";
  private static final String SPRING_ES_PASSWORD_PROP = "spring.elasticsearch.password";
  private static final String SPRING_ES_INDEX_PROP = "spring.elasticsearch.index.name";
  private static final String SPRING_NEO4J_URI_PROP = "spring.neo4j.uri";
  private static final String SPRING_NEO4J_USERNAME_PROP = "spring.neo4j.authentication.username";
  private static final String SPRING_NEO4J_PASSWORD_PROP = "spring.neo4j.authentication.password";
  private static final String SPRING_CONCEPT_GRAPH_URI_PROP = "top.documents.concept-graphs-api.uri";
  private static final String TOP_SECURITY_GRAPHDB_USERNAME = "top.documents.security.graphdb.username";
  private static final String TOP_SECURITY_GRAPHDB_PASSWORD = "top.documents.security.graphdb.password";
  private static final String TOP_SECURITY_DOCUMENTDB_USERNAME = "top.documents.security.documentdb.username";
  private static final String TOP_SECURITY_DOCUMENTDB_PASSWORD = "top.documents.security.documentdb.password";
  private static final String TOP_DATA_SOURCE_CONFIG_DIR_DOCUMENTS = "top.documents.data-source-config-dir";
  private static final String TOP_DEFAULT_ADAPTER_DOCUMENTS = "top.documents.default_adapter";
  private static final Logger LOGGER = Logger.getLogger(DocumentQueryConfigMap.class.getName());
  private Map<String, Object> configMap;
  private String name;

  public Map<String, Object> getConfigMap() {
    return configMap;
  }
  public void setConfigMap(Map<String, Object> configMap) {
    this.configMap = configMap;
  }

  public void setConfigMap(TextAdapterConfig config, Environment environment) {
    this.configMap =
        new HashMap<>() {
          {
            if (config.getConnection() != null) {
              put(
                  SPRING_ES_URI_PROP,
                  config.getConnection().getUrl() + ":" + config.getConnection().getPort());
            } else {
              put(SPRING_ES_URI_PROP, DEFAULT_ES_URL + ":" + DEFAULT_ES_PORT);
            }
            if (config.getIndex() != null) {
              // ToDo: look into this - provide list here possible, too?
              put(
                  SPRING_ES_INDEX_PROP,
                  Arrays.stream(config.getIndex()).findFirst().orElse(DEFAULT_ES_INDEX));
            } else {
              put(SPRING_ES_INDEX_PROP, DEFAULT_ES_INDEX);
            }
            if (config.getGraphDB() != null) {
              if (config.getGraphDB().getConnection() != null) {
                String url =
                    config.getGraphDB().getConnection().getUrl() != null
                        ? config.getGraphDB().getConnection().getUrl()
                        : DEFAULT_NEO4J_URL;
                String port =
                    config.getGraphDB().getConnection().getPort() != null
                        ? config.getGraphDB().getConnection().getPort()
                        : DEFAULT_NEO4J_PORT;
                put(SPRING_NEO4J_URI_PROP, url + ":" + port);
              } else {
                put(SPRING_NEO4J_URI_PROP, DEFAULT_NEO4J_URL + ":" + DEFAULT_NEO4J_PORT);
              }
              put(
                  SPRING_NEO4J_USERNAME_PROP,
                  (config.getGraphDB().getAuthentication() == null
                          || config.getGraphDB().getAuthentication().getUsername() == null)
                      ? environment.getProperty(TOP_SECURITY_GRAPHDB_USERNAME)
                      : config.getGraphDB().getAuthentication().getUsername());
              put(
                  SPRING_NEO4J_PASSWORD_PROP,
                  (config.getGraphDB().getAuthentication() == null
                          || config.getGraphDB().getAuthentication().getPassword() == null)
                      ? environment.getProperty(TOP_SECURITY_GRAPHDB_PASSWORD)
                      : config.getGraphDB().getAuthentication().getPassword());
            }
            if (config.getConceptGraph() != null) {
              if (config.getConceptGraph().getConnection() != null) {
                String url =
                    config.getConceptGraph().getConnection().getUrl() != null
                        ? config.getConceptGraph().getConnection().getUrl()
                        : DEFAULT_CONCEPT_GRAPH_URL;
                String port =
                    config.getConceptGraph().getConnection().getPort() != null
                        ? config.getConceptGraph().getConnection().getPort()
                        : DEFAULT_CONCEPT_GRAPH_PORT;
                put(SPRING_CONCEPT_GRAPH_URI_PROP, url + ":" + port);
              } else {
                put(SPRING_CONCEPT_GRAPH_URI_PROP, DEFAULT_CONCEPT_GRAPH_URL + ":" + DEFAULT_CONCEPT_GRAPH_PORT);
              }
            }
          }
        };
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DocumentQueryConfigMap(Environment environment) {
    String srcPath = environment.getProperty(TOP_DATA_SOURCE_CONFIG_DIR_DOCUMENTS);
    String defaultAdapter = environment.getProperty(TOP_DEFAULT_ADAPTER_DOCUMENTS);
    TextAdapterConfig config =
            !Objects.equals(defaultAdapter, "#{null}") ?
            toTextAdapterConfig(Path.of(srcPath != null ? srcPath : Paths.get("").toAbsolutePath().toString(), defaultAdapter)) :
            getFirstTextAdapterConfig(srcPath);
    if (config == null) config = defaultConfig(environment);
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

  private TextAdapterConfig defaultConfig(Environment env) {
    TextAdapterConfig config = new TextAdapterConfig();
    config.setId("Default Adapter");

    Connection elasticConnection = new Connection();

    elasticConnection.setUrl(DEFAULT_ES_URL);
    elasticConnection.setPort(DEFAULT_ES_PORT);
    if (env.getProperty(SPRING_ES_URI_PROP) != null) {
      URL url;
      try {
        url = new URL(env.getProperty(SPRING_ES_URI_PROP));
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      elasticConnection.setUrl(url.getHost());
      elasticConnection.setPort(url.getPort() != -1 ? String.valueOf(url.getPort()) : DEFAULT_ES_PORT);
    }
    config.setConnection(elasticConnection);

    config.setIndex(new String[]{ DEFAULT_ES_INDEX });
    if (env.getProperty(SPRING_ES_INDEX_PROP) != null) config.setIndex(new String[]{ env.getProperty(SPRING_ES_INDEX_PROP) });
    config.setField(new String[]{DEFAULT_ES_FIELD});

    Connection neo4JConnection = new Connection();
    neo4JConnection.setUrl(DEFAULT_NEO4J_URL);
    neo4JConnection.setPort(DEFAULT_NEO4J_PORT);
    if (env.getProperty(SPRING_NEO4J_URI_PROP) != null) {
      URL url;
      try {
        url = new URL(env.getProperty(SPRING_NEO4J_URI_PROP));
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      neo4JConnection.setUrl(url.getHost());
      neo4JConnection.setPort(url.getPort() != -1 ? String.valueOf(url.getPort()) : DEFAULT_NEO4J_PORT);
    }
    GraphDBConfig neo4j = new GraphDBConfig();
    neo4j.setConnection(neo4JConnection);
    config.setGraphDB(neo4j);

    Connection conceptGraphConnection = new Connection();
    conceptGraphConnection.setUrl(DEFAULT_CONCEPT_GRAPH_URL);
    conceptGraphConnection.setPort(DEFAULT_CONCEPT_GRAPH_PORT);
    if (env.getProperty(SPRING_CONCEPT_GRAPH_URI_PROP) != null) {
      URL url;
      try {
        url = new URL(env.getProperty(SPRING_CONCEPT_GRAPH_URI_PROP));
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      conceptGraphConnection.setUrl(url.getHost());
      conceptGraphConnection.setPort(url.getPort() != -1 ? String.valueOf(url.getPort()) : DEFAULT_CONCEPT_GRAPH_PORT);
    }
    ConceptGraphConfig conceptGraph = new ConceptGraphConfig();
    conceptGraph.setConnection(conceptGraphConnection);
    config.setConceptGraph(conceptGraph);

    return config;
  }
}

