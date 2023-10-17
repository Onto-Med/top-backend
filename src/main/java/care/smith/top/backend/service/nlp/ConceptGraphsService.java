package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.GraphStatsEntity;
import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphStat;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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
    return 0;
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
}
