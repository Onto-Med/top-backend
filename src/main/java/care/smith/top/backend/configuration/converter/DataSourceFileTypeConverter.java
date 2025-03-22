package care.smith.top.backend.configuration.converter;

import care.smith.top.model.DataSourceFileType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DataSourceFileTypeConverter implements Converter<String, DataSourceFileType> {
  @Override
  public DataSourceFileType convert(@NonNull String source) {
    try {
      return DataSourceFileType.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
