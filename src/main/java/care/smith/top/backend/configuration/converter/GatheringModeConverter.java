package care.smith.top.backend.configuration.converter;

import care.smith.top.model.DocumentGatheringMode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class GatheringModeConverter implements Converter<String, DocumentGatheringMode> {
  @Override
  public DocumentGatheringMode convert(@NonNull String source) {
    try {
      return DocumentGatheringMode.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
