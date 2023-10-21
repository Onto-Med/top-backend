package care.smith.top.backend.repository.conceptgraphs;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphEntity;
import care.smith.top.backend.model.conceptgraphs.ConceptGraphStatisticsEntity;
import care.smith.top.backend.model.conceptgraphs.ProcessOverviewEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.logging.Logger;

@Repository
public class ConceptGraphsRepository extends ConceptGraphsApi {
  private static final Logger LOGGER = Logger.getLogger(ConceptGraphsRepository.class.getName());

  @Cacheable(value = "conceptGraphStoredProcesses", unless = " #result == null ")
  public ProcessOverviewEntity getAllStoredProcesses() {
    try {
      return conceptGraphsApi
          .get()
          .uri(uriBuilder -> uriBuilder.path(API_PROCESS_METHODS.STATISTICS.getEndpoint()).build())
          .retrieve()
          .bodyToMono(ProcessOverviewEntity.class)
          .block();
    } catch (WebClientResponseException e) {
      LOGGER.warning(e.getResponseBodyAsString() + " -- " + e.getMessage());
      return null;
    }
  }

  @Cacheable(value = "conceptGraphStatistics", unless = " #result == null ")
  public ConceptGraphStatisticsEntity getGraphStatisticsForProcess(String processName) {
    try {
      return conceptGraphsApi
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(API_GRAPH_METHODS.STATISTICS.getEndpoint())
                          .queryParam("process", processName)
                          .build())
              .retrieve()
              .bodyToMono(ConceptGraphStatisticsEntity.class)
              .block();
    } catch (WebClientResponseException e) {
      LOGGER.warning(e.getResponseBodyAsString() + " -- " + e.getMessage());
      return null;
    }
  }

  public ConceptGraphEntity getGraphForIdAndProcess(String id, String processName) {
    try {
      return conceptGraphsApi
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(API_GRAPH_METHODS.GRAPH.getEndpoint(id))
                          .queryParam("process", processName)
                          .build())
              .retrieve()
              .bodyToMono(ConceptGraphEntity.class)
              .block();
    } catch (WebClientResponseException e) {
      LOGGER.warning(e.getResponseBodyAsString() + " -- " + e.getMessage());
      return null;
    }
  }
}
