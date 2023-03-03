package care.smith.top.backend.configuration.converter;

import care.smith.top.backend.model.Permission;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PermissionConverter implements Converter<String, Permission> {
  @Override
  public Permission convert(@NonNull String source) {
    try {
      return Permission.valueOf(source);
    } catch (Exception e) {
      return null;
    }
  }
}
