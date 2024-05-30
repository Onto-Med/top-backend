package care.smith.top.backend.configuration.converter;

import care.smith.top.model.ConceptGraphPipelineStepsEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PipelineStepsConverter implements Converter<String, ConceptGraphPipelineStepsEnum> {
  @Override
  public ConceptGraphPipelineStepsEnum convert(@NonNull String source) {
    try {
      return ConceptGraphPipelineStepsEnum.fromValue(source);
    } catch (Exception e) {
      return null;
    }
  }
}
