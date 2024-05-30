package care.smith.top.backend.configuration.converter;

import care.smith.top.model.ConceptGraphPipelineStatusEnum;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PipelineStatusConverter implements Converter<String, ConceptGraphPipelineStatusEnum> {
  @Override
  public ConceptGraphPipelineStatusEnum convert(@NotNull String source) {
    try {
      return ConceptGraphPipelineStatusEnum.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
