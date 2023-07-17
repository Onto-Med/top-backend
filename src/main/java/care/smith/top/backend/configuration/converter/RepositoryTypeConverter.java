package care.smith.top.backend.configuration.converter;

import care.smith.top.model.RepositoryType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RepositoryTypeConverter implements Converter<String, RepositoryType> {
  @Override
  public RepositoryType convert(@NonNull String source) {
    try {
      return RepositoryType.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
