package care.smith.top.backend.configuration.converter;

import care.smith.top.model.Purpose;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PurposeConverter implements Converter<String, Purpose> {
  @Override
  public Purpose convert(@NonNull String source) {
    try {
      return Purpose.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
