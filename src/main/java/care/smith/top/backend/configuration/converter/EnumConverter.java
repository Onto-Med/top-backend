package care.smith.top.backend.configuration.converter;

import care.smith.top.model.QueryType;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class EnumConverter implements WebMvcConfigurer {
  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new QueryTypeEnumConverter());
  }

  public static class QueryTypeEnumConverter implements ConverterFactory<String, QueryType> {

    @Override
    public <T extends QueryType> @NotNull Converter<String, T> getConverter(
        @NotNull Class<T> targetType) {
      return new QueryTypeStringToEnumConverter<>();
    }

    public static class QueryTypeStringToEnumConverter<T extends QueryType>
        implements Converter<String, T> {

      public QueryTypeStringToEnumConverter() {}

      @SuppressWarnings("unchecked")
      @Override
      public T convert(String source) {
        return (T) QueryType.fromValue(source);
      }
    }
  }
}
