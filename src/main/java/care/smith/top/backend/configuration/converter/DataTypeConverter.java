package care.smith.top.backend.configuration.converter;

import care.smith.top.model.DataType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DataTypeConverter implements Converter<String, DataType> {
  @Override
  public DataType convert(@NonNull String source) {
    try {
      return DataType.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
