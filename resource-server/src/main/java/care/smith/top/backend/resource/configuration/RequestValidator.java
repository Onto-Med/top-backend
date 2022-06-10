package care.smith.top.backend.resource.configuration;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class RequestValidator {
  public static boolean isValidId(String id) {
    return StringUtils.isNoneBlank(id)
        && id.matches("^[\\w\\-]+$") // allow alphanumeric, underscore and dash
        && !Arrays.asList("organisation", "repository", "entity", "ontology")
            .contains(id.toLowerCase());
  }
}
