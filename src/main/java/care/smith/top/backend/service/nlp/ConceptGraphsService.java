package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphStatisticsEntity;
import care.smith.top.backend.model.conceptgraphs.GraphStatsEntity;
import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import care.smith.top.model.PipelineResponse;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ConceptGraphsService implements ContentService {

  private final ConceptGraphsRepository conceptGraphsRepository;

  public ConceptGraphsService(ConceptGraphsRepository conceptGraphsRepository) {
    this.conceptGraphsRepository = conceptGraphsRepository;
  }

  @Override
  public long count() {
    return Arrays.stream(conceptGraphsRepository.getAllStoredProcesses().getProcesses()).count();
  }

  public Map<String, ConceptGraphStat> getAllConceptGraphStatistics(String processName) {
    ConceptGraphStatisticsEntity conceptGraphStatisticsEntity = conceptGraphsRepository.getGraphStatisticsForProcess(processName);
    if (conceptGraphStatisticsEntity.getConceptGraphs() == null) return null;
    return Arrays.stream(conceptGraphStatisticsEntity.getConceptGraphs())
        .map(GraphStatsEntity::toApiModel)
        .collect(
            Collectors.toMap(
                ConceptGraphStat::getId, Function.identity(), (existing, replacement) -> existing));
  }

  public ConceptGraph getConceptGraphForIdAndProcess(String id, String process) {
    return conceptGraphsRepository.getGraphForIdAndProcess(id, process).toApiModel();
  }

  public List<ConceptGraphProcess> getAllStoredProcesses() {
    return conceptGraphsRepository.getAllStoredProcesses().toApiModel();
  }

  public PipelineResponse initPipeline(File data, String processName) {
    return conceptGraphsRepository
        .startPipelineForData(data, processName, null, true, false)
        .getSpecificResponse();
  }

  public PipelineResponse initPipelineWithBooleans(
      File data, String processName, Boolean skipPresent, Boolean returnStatistics) {
    return conceptGraphsRepository
        .startPipelineForData(data, processName, null, skipPresent, returnStatistics)
        .getSpecificResponse();
  }

  public PipelineResponse initPipelineWithConfigs(
      File data,
      File labels,
      String processName,
      String language,
      Boolean skipPresent,
      Boolean returnStatistics,
      Map<String, File> configs) {
    return conceptGraphsRepository
        .startPipelineForDataAndLabelsAndConfigs(
            data, labels, processName, language, skipPresent, returnStatistics, configs)
        .getSpecificResponse();
  }
}
