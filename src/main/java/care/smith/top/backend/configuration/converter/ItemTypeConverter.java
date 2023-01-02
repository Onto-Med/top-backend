package care.smith.top.backend.configuration.converter;

import care.smith.top.model.ItemType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ItemTypeConverter implements Converter<String, ItemType> {
  @Override
  public ItemType convert(@NonNull String source) {
    try {
      return ItemType.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
