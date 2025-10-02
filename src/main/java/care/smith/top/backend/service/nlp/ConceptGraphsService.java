package care.smith.top.backend.service.nlp;

import care.smith.top.backend.service.ContentService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.concept_graphs_api.ConceptPipelineManager;
import care.smith.top.top_document_query.concept_graphs_api.model.ConceptGraphEntity;
import care.smith.top.top_document_query.concept_graphs_api.model.GraphStatsEntity;
import care.smith.top.top_document_query.concept_graphs_api.model.pipeline_response.PipelineFailEntity;
import care.smith.top.top_document_query.concept_graphs_api.model.pipeline_response.PipelineResponseEntity;
import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConceptGraphsService implements ContentService {
  private final ConceptPipelineManager pipelineManager;
  private static final Logger LOGGER = Logger.getLogger(ConceptGraphsService.class.getName());

  public ConceptGraphsService(
      @Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri) {
    ConceptPipelineManager tmpPipeline;
    try {
      tmpPipeline = new ConceptPipelineManager(conceptGraphsApiUri);
    } catch (MalformedURLException e) {
      try {
        tmpPipeline = new ConceptPipelineManager("http://localhost:9010");
      } catch (MalformedURLException ex) {
        throw new RuntimeException(ex);
      }
    }
    pipelineManager = tmpPipeline;
  }

  @Override
  public long count() {
    return pipelineManager.count();
  }

  public Map<String, ConceptGraphStat> getAllConceptGraphStatistics(String processName) {
    return pipelineManager
        .getGraphStatisticsForProcess(processName)
        .map(
            statistics -> {
              if (statistics.getConceptGraphs() == null)
                return new HashMap<String, ConceptGraphStat>();
              return Arrays.stream(statistics.getConceptGraphs())
                  .map(GraphStatsEntity::toApiModel)
                  .collect(
                      Collectors.toMap(
                          ConceptGraphStat::getId,
                          Function.identity(),
                          (existing, replacement) -> existing));
            })
        .orElseGet(HashMap::new);
  }

  public ConceptGraph getConceptGraphForIdAndProcess(String id, String process) {
    return pipelineManager
        .getGraphForIdAndProcess(id, process)
        .map(ConceptGraphEntity::toApiModel)
        .orElse(null);
  }

  public List<ConceptGraphPipeline> getAllStoredProcesses() {
    return pipelineManager.getAllStoredProcesses();
  }

  public PipelineResponseStatus getStatusOfPipeline(String process) {
    return pipelineManager
        .getStatusOfProcess(process)
        .orElseThrow()
        .getSpecificResponse()
        .getStatus();
  }

  public PipelineResponse deletePipeline(String processId) {
    PipelineResponse pipelineResponse = new PipelineResponse().pipelineId(processId);
    String stringResponse = pipelineManager.deleteProcess(processId);
    if (stringResponse.toLowerCase().contains("no such process")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
    } else if (stringResponse.toLowerCase().contains("set to be deleted")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.SUCCESSFUL);
    }
    return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
  }

  public String getPipelineConfig(String processId, String language) {
    return pipelineManager.getPipelineConfiguration(processId, language).orElse("{}");
  }

  public PipelineResponse initPipeline(
      File data,
      File labels,
      Map<String, File> configs,
      String processName,
      String language,
      Boolean skipPresent,
      Boolean returnStatistics) {
    PipelineResponseEntity pre =
        pipelineManager.startPipeline(
            data, configs, labels, processName, language, skipPresent, returnStatistics);
    return addStatusToPipelineResponse(pre, processName);
  }

  public PipelineResponse initPipeline(
      String processName,
      String language,
      Boolean skipPresent,
      Boolean returnStatistics,
      JSONObject jsonBody,
      JSONObject cgApiUrl) {
    String url = cgApiUrl.getString("url") + ":" + cgApiUrl.getString("port");
    try {
      boolean success = pipelineManager.switchConnection(url);
      if (!success) {
        if (cgApiUrl.has("alternate_url") && cgApiUrl.get("alternate_url") != JSONObject.NULL) {
          String alternateUrl =
              cgApiUrl.getString("alternate_url") + ":" + cgApiUrl.getString("port");
          success = pipelineManager.switchConnection(alternateUrl);
        }
        if (!success) {
          LOGGER.severe(
              "Tried to initiate PipelineManager with connection info given in Adapter config but to no avail."
                  + " Trying to use the default as given in the backend application's config.");
        }
      }
    } catch (MalformedURLException e) {
      LOGGER.warning(
          "The given url in the Adapter config seems to be malformed: '"
              + url
              + "'; or the alternate url is malformed/missing.");
    }
    PipelineResponseEntity pre =
        pipelineManager.startPipeline(
            processName, language, skipPresent, returnStatistics, jsonBody);
    return addStatusToPipelineResponse(pre, processName);
  }

  public PipelineResponse stopPipeline(String processName) {
    PipelineResponse pipelineResponse = new PipelineResponse().pipelineId(processName);
    String stringResponse = pipelineManager.stopPipeline(processName);
    if (stringResponse.toLowerCase().contains("no thread/process for")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
    } else if (stringResponse.toLowerCase().contains("find a running step in the pipeline")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
    } else if (stringResponse.toLowerCase().contains("process will be stopped")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.SUCCESSFUL);
    }
    return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
  }

  private PipelineResponse addStatusToPipelineResponse(
      PipelineResponseEntity responseEntity, String processName) {
    if (responseEntity == null) {
      return new PipelineResponse()
          .pipelineId(processName)
          .response("Pipeline failed. Please consult the logs.")
          .status(PipelineResponseStatus.FAILED);
    } else if (responseEntity instanceof PipelineFailEntity) {
      return responseEntity.getSpecificResponse().status(PipelineResponseStatus.FAILED);
    }
    return responseEntity.getSpecificResponse().status(PipelineResponseStatus.SUCCESSFUL);
  }
}
