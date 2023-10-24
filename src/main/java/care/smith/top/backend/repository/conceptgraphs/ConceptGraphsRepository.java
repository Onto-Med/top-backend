package care.smith.top.backend.repository.conceptgraphs;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphEntity;
import care.smith.top.backend.model.conceptgraphs.ConceptGraphStatisticsEntity;
import care.smith.top.backend.model.conceptgraphs.ProcessOverviewEntity;
import org.hibernate.service.spi.ServiceException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;

@Repository
public class ConceptGraphsRepository extends ConceptGraphsApi {
  private static final Logger LOGGER = Logger.getLogger(ConceptGraphsRepository.class.getName());

  @Cacheable(value = "conceptGraphStoredProcesses", unless = " #result == null ")
  public ProcessOverviewEntity getAllStoredProcesses() {
    try {
      return conceptGraphsApi
          .get()
          .uri(uriBuilder -> uriBuilder.path(API_PROCESS_METHODS.ALL.getEndpoint()).build())
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

  public ConceptGraphStatisticsEntity startPipelineForData(
      @Nonnull File data,
      @Nonnull String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent
  ) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("data", new FileSystemResource(data));
    try {
      Mono<ConceptGraphStatisticsEntity> apiResponse = conceptGraphsApi
          .post()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path(API_PIPELINE_METHODS.INITIALIZE.getEndpoint())
                      .queryParam("process", processName)
                      .queryParam("lang", language == null ? "en" : language)
                      .queryParam("skip_present", skipPresent == null || skipPresent)
                      .build())
          .body(BodyInserters.fromMultipartData(parts))
          .retrieve()
          .bodyToMono(ConceptGraphStatisticsEntity.class);
//          .exchangeToMono(response -> response.bodyToMono(ConceptGraphStatisticsEntity.class));
      return apiResponse.block(); //ToDo: now I need some way of accessing the status of the pipeline...
    } catch (WebClientResponseException e) {
      LOGGER.warning(e.getResponseBodyAsString() + " -- " + e.getMessage());
      return null;
    }
  }

  public String startPipelineForDataAndLabels(
      Object data,
      Object labels,
      String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent
  ) {
    return null;
  }

  public String startPipelineForDataAndConfigs(
      Object data,
      @Nullable String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent,
      Object... configs
  ) {
      return null;
    }

  public String startPipelineForDataAndLabelsAndConfigs(
      Object data,
      Object labels,
      @Nullable String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent,
      Object... configs
  ) {
    return null;
  }
}
