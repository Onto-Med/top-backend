package care.smith.top.backend.configuration.converter;

import care.smith.top.model.EntityType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class EntityTypeConverter implements Converter<String, EntityType> {
  @Override
  public EntityType convert(@NonNull String source) {
    try {
      return EntityType.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
