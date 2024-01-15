package care.smith.top.backend.repository.conceptgraphs;

import care.smith.top.backend.model.conceptgraphs.*;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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

  public PipelineResponseEntity startPipelineForData(
      @Nonnull File data,
      @Nonnull String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent,
      @Nullable Boolean returnStatistics) {
    return startPipelineForDataAndLabelsAndConfigs(
        data, null, processName, language, skipPresent, returnStatistics, null);
  }

  public PipelineResponseEntity startPipelineForDataAndLabels(
      @Nonnull File data,
      @Nonnull File labels,
      @Nonnull String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent,
      @Nullable Boolean returnStatistics) {
    return startPipelineForDataAndLabelsAndConfigs(
        data, labels, processName, language, skipPresent, returnStatistics, null);
  }

  public PipelineResponseEntity startPipelineForDataAndConfigs(
      @Nonnull File data,
      @Nullable String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent,
      @Nullable Boolean returnStatistics,
      @Nonnull Map<String, File> configs) {
    return startPipelineForDataAndLabelsAndConfigs(
        data, null, processName, language, skipPresent, returnStatistics, configs);
  }

  public PipelineResponseEntity startPipelineForDataAndLabelsAndConfigs(
      @Nonnull File data,
      File labels,
      @Nullable String processName,
      @Nullable String language,
      @Nullable Boolean skipPresent,
      @Nullable Boolean returnStatistics,
      Map<String, File> configs) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("data", new FileSystemResource(data));
    if (labels != null) parts.add("labels", new FileSystemResource(labels));
    if (configs != null && !configs.isEmpty())
      configs.forEach((name, file) -> parts.add(name + "_config", new FileSystemResource(file)));

    try {
      Mono<PipelineResponseEntity> apiResponse =
          conceptGraphsApi
              .post()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(API_PIPELINE_METHODS.INITIALIZE.getEndpoint())
                          .queryParam("process", processName)
                          .queryParam("lang", language == null ? "en" : language)
                          .queryParam("skip_present", skipPresent == null || skipPresent)
                          .queryParam(
                              "return_statistics", returnStatistics != null && returnStatistics)
                          .build())
              .body(BodyInserters.fromMultipartData(parts))
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(ConceptGraphStatisticsEntity.class);
                    } else if (response.statusCode().equals(HttpStatus.ACCEPTED)) {
                      return response.bodyToMono(PipelineStatusEntity.class);
                    } else {
                      return response.bodyToMono(PipelineFailEntity.class);
                    }
                  });
      return apiResponse.block();
    } catch (WebClientResponseException e) {
      LOGGER.warning(e.getResponseBodyAsString() + " -- " + e.getMessage());
      return null;
    }
  }
}
