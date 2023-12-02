package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.GraphStatsEntity;
import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import care.smith.top.model.PipelineResponse;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
      return Arrays.stream(conceptGraphsRepository.getGraphStatisticsForProcess(processName).getConceptGraphs())
          .map(GraphStatsEntity::toApiModel)
          .collect(
              Collectors.toMap(
                  ConceptGraphStat::getId,
                  Function.identity(),
                  (existing, replacement) -> existing)
          );
  }

  public ConceptGraph getConceptGraphForIdAndProcess(String id, String process) {
    return conceptGraphsRepository.getGraphForIdAndProcess(id, process).toApiModel();
  }

  public List<ConceptGraphProcess> getAllStoredProcesses() {
    return conceptGraphsRepository.getAllStoredProcesses().toApiModel();
  }

//  public Map<String, ConceptGraphStat> initPipeline(File data, String processName) {
//      return Arrays.stream(conceptGraphsRepository.startPipelineForData(data, processName, null, true)
//          .getConceptGraphs())
//          .map(GraphStatsEntity::toApiModel)
//          .collect(
//              Collectors.toMap(
//                  ConceptGraphStat::getId,
//                  Function.identity(),
//                  (existing, replacement) -> existing)
//          );
//    }
//
//  public Map<String, ConceptGraphStat> initPipelineWithConfigs(
//      File data,
//      File labels,
//      String processName,
//      String language,
//      Boolean skipPresent,
//      Map<String, File> configs
//  ) {
//    return Arrays.stream(conceptGraphsRepository.startPipelineForDataAndLabelsAndConfigs(
//        data, labels, processName, language, skipPresent, configs).getConceptGraphs())
//        .map(GraphStatsEntity::toApiModel)
//        .collect(
//            Collectors.toMap(
//                ConceptGraphStat::getId,
//                Function.identity(),
//                (existing, replacement) -> existing)
//        );
//  }

  public PipelineResponse initPipeline(File data, String processName) {
      return conceptGraphsRepository.startPipelineForData(data, processName, null, true)
          .getResponse();
    }

  public PipelineResponse initPipelineWithConfigs(
      File data,
      File labels,
      String processName,
      String language,
      Boolean skipPresent,
      Map<String, File> configs
  ) {
    return Arrays.stream(conceptGraphsRepository.startPipelineForDataAndLabelsAndConfigs(
        data, labels, processName, language, skipPresent, configs).getConceptGraphs())
        .map(GraphStatsEntity::toApiModel)
        .collect(
            Collectors.toMap(
                ConceptGraphStat::getId,
                Function.identity(),
                (existing, replacement) -> existing)
        );
  }
}

