package care.smith.top.backend.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Permission {
  READ,
  WRITE,
  MANAGE;

  public static List<Permission> getIncluding(Permission permission) {
    return Arrays.stream(Permission.values())
        .filter(p -> p.ordinal() >= permission.ordinal())
        .collect(Collectors.toList());
  }

  public static Permission fromApiModel(care.smith.top.model.Permission permission) {
    return Permission.valueOf(permission.getValue());
  }

  public static care.smith.top.model.Permission toApiModel(
      Permission permission) {
    return care.smith.top.model.Permission.valueOf(permission.name());
  }
}
