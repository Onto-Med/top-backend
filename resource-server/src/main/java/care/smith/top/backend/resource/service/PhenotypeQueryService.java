package care.smith.top.backend.resource.service;

import care.smith.top.top_phenotypic_query.adapter.config.DataAdapterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PhenotypeQueryService {
  private static final Logger LOGGER = Logger.getLogger(PhenotypeQueryService.class.getName());

  @Value("${top.phenotyping.data-source-config-dir:config/data_sources}")
  private String dataSourceConfigDir;

  public DataAdapterConfig getDataAdapterConfig(String id) {
    if (id == null) return null;
    return getDataAdapterConfigs().stream()
        .filter(a -> id.equals(a.getId()))
        .findFirst()
        .orElse(null);
  }

  public List<DataAdapterConfig> getDataAdapterConfigs() {
    try (Stream<Path> paths = Files.list(Path.of(dataSourceConfigDir))) {
      return paths
          .map(this::toDataAdapterConfig)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (Exception ignored) {
    }
    return Collections.emptyList();
  }

  private DataAdapterConfig toDataAdapterConfig(Path path) {
    try {
      return DataAdapterConfig.getInstance(path.toString());
    } catch (Exception e) {
      LOGGER.warning(
          String.format(
              "Data adapter config could not be loaded from file '%s'. Error: %s",
              path.toString(), e.getMessage()));
    }
    return null;
  }
}
