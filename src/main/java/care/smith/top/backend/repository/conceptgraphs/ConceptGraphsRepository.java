package care.smith.top.backend.repository.conceptgraphs;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphStatisticsEntity;
import care.smith.top.model.ConceptGraphStat;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Repository
public class ConceptGraphsRepository extends ConceptGraphsApi {
  private static final Logger LOGGER = Logger.getLogger(ConceptGraphsRepository.class.getName());

  @Cacheable("conceptGraphStatistics")
  public Map<String, ConceptGraphStat> getGraphStatisticsForProcess(String processName) {
    ConceptGraphStatisticsEntity graphStatistics =
        conceptGraphsApi
            .get()
            .uri(
                uriBuilder ->
                  uriBuilder
                      .path(API_GRAPH_METHODS.STATISTICS.getEndpoint())
                      .queryParam("process",processName)
                      .build())
            .retrieve()
            .bodyToMono(ConceptGraphStatisticsEntity.class)
            .block();

    return Arrays.stream(Objects.requireNonNull(graphStatistics).getConceptGraphs())
        .map(
            graphStats ->
                new ConceptGraphStat()
                    .id(graphStats.getId())
                    .nodes(graphStats.getNodes())
                    .edges(graphStats.getEdges()))
        .collect(
            Collectors.toMap(
              ConceptGraphStat::getId, Function.identity(), (existing, replacement) -> existing));
  }
}
