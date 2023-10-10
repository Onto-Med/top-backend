package care.smith.top.backend.configuration.nlp;

import org.springframework.core.env.MapPropertySource;

public class DocumentQueryPropertySource extends MapPropertySource {
  public DocumentQueryPropertySource(DocumentQueryConfigMap configMap) {
    super(configMap.getName(), configMap.getConfigMap());
  }
}
