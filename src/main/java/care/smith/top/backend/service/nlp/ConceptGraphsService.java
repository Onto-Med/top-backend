package care.smith.top.backend.service.nlp;

import care.smith.top.backend.service.ContentService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.concept_cluster.ConceptPipelineManager;
import care.smith.top.top_document_query.concept_cluster.model.ConceptGraphEntity;
import care.smith.top.top_document_query.concept_cluster.model.GraphStatsEntity;
import care.smith.top.top_document_query.concept_cluster.model.pipeline_response.PipelineFailEntity;
import care.smith.top.top_document_query.concept_cluster.model.pipeline_response.PipelineResponseEntity;
import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConceptGraphsService implements ContentService {
  private final ConceptPipelineManager pipelineManager;

  public ConceptGraphsService(
      @Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri) {
    pipelineManager = new ConceptPipelineManager(conceptGraphsApiUri);
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
    // ToDo: need to implement proper checkable status in concept-graphs-api
    PipelineResponse pipelineResponse = new PipelineResponse().pipelineId(processId);
    String stringResponse = pipelineManager.deleteProcess(processId);
    if (stringResponse.toLowerCase().contains("no such process")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
    } else if (stringResponse.toLowerCase().contains("is currently running")) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.RUNNING);
    } else if (stringResponse
        .toLowerCase()
        .contains(String.format("process '%s' deleted", processId.toLowerCase()))) {
      return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.SUCCESSFUL);
    }
    return pipelineResponse.response(stringResponse).status(PipelineResponseStatus.FAILED);
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
