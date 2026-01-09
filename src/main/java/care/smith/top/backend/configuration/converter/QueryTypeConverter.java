package care.smith.top.backend.configuration.converter;

import care.smith.top.model.QueryType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class QueryTypeConverter implements Converter<String, QueryType> {
  @Override
  public QueryType convert(@NonNull String source) {
    try {
      return QueryType.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
