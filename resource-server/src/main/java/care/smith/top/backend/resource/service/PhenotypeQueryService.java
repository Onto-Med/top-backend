package care.smith.top.backend.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PhenotypeQueryService {
  @Value("${top.phenotyping.data-source-config-dir:config/data_sources}")
  private String dataSourceConfigDir;

  public List<Path> getDataSources() {
    try (Stream<Path> files = Files.list(Path.of(dataSourceConfigDir))) {
      return files.collect(Collectors.toList());
    } catch (Exception ignore) {
    }
    return Collections.emptyList();
  }
}
