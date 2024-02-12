package care.smith.top.backend.service.nlp;

import care.smith.top.backend.service.ContentService;
import care.smith.top.model.*;
import care.smith.top.top_document_query.concept_cluster.ConceptPipelineManager;
import care.smith.top.top_document_query.concept_cluster.model.ConceptGraphStatisticsEntity;
import care.smith.top.top_document_query.concept_cluster.model.GraphStatsEntity;
import care.smith.top.top_document_query.concept_cluster.model.pipeline_response.PipelineFailEntity;
import care.smith.top.top_document_query.concept_cluster.model.pipeline_response.PipelineResponseEntity;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConceptGraphsService implements ContentService {

  @Value("${spring.elasticsearch.uris}")
  private String documentServerAddress;

  @Value("${spring.elasticsearch.index.name}")
  private String documentServerIndexName;

  @Value("${top.documents.document-server.batch-size:30}")
  private Integer documentServerBatchSize;

  @Value("${top.documents.document-server.fields-replacement:{'text': 'content'}}")
  private String documentServerFieldsReplacement;

  @Value("${top.documents.concept-graphs-api.uri:http://localhost:9007}")
  private String conceptGraphsApiUri;

  private final ConceptPipelineManager pipelineManager =
      new ConceptPipelineManager(conceptGraphsApiUri);

  public String getDocumentServerAddress() {
    return documentServerAddress;
  }

  public String getDocumentServerIndexName() {
    return documentServerIndexName;
  }

  public Integer getDocumentServerBatchSize() {
    return documentServerBatchSize;
  }

  public String getDocumentServerFieldsReplacement() {
    return documentServerFieldsReplacement;
  }

  public Map<String, String> getDocumentServerFieldsReplacementAsMap() {
    return Arrays.stream(documentServerFieldsReplacement.split(","))
        .map(entry -> entry.split(":"))
        .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
  }

  @Override
  public long count() {
    return pipelineManager.count();
  }

  public Map<String, ConceptGraphStat> getAllConceptGraphStatistics(String processName) {
    ConceptGraphStatisticsEntity conceptGraphStatisticsEntity =
        pipelineManager.getGraphStatisticsForProcess(processName);
    if (conceptGraphStatisticsEntity.getConceptGraphs() == null) return null;
    return Arrays.stream(conceptGraphStatisticsEntity.getConceptGraphs())
        .map(GraphStatsEntity::toApiModel)
        .collect(
            Collectors.toMap(
                ConceptGraphStat::getId, Function.identity(), (existing, replacement) -> existing));
  }

  public ConceptGraph getConceptGraphForIdAndProcess(String id, String process) {
    return pipelineManager.getGraphForIdAndProcess(id, process).toApiModel();
  }

  public List<ConceptGraphProcess> getAllStoredProcesses() {
    return pipelineManager.getAllStoredProcesses();
  }

  public PipelineResponse initPipeline(File data, String processName) {
    PipelineResponseEntity pre =
        pipelineManager.startPipelineForData(data, processName, null, true, false);
    return addStatusToPipelineResponse(pre, processName);
  }

  public PipelineResponse initPipelineWithBooleans(
      File data, String processName, Boolean skipPresent, Boolean returnStatistics) {
    PipelineResponseEntity pre =
        pipelineManager.startPipelineForData(
            data, processName, null, skipPresent, returnStatistics);
    return addStatusToPipelineResponse(pre, processName);
  }

  public PipelineResponse initPipelineWithDataUploadAndWithConfigs(
      File data,
      File labels,
      String processName,
      String language,
      Boolean skipPresent,
      Boolean returnStatistics,
      Map<String, File> configs) {
    PipelineResponseEntity pre =
        pipelineManager.startPipelineForDataAndLabelsAndConfigs(
            data, labels, processName, language, skipPresent, returnStatistics, configs);
    return addStatusToPipelineResponse(pre, processName);
  }

  public PipelineResponse initPipelineWithDataServerAndWithConfigs(
      File labels,
      String processName,
      String lang,
      Boolean skipPresent,
      Boolean returnStatistics,
      Map<String, File> configs) {
    PipelineResponseEntity pre =
        pipelineManager.startPipelineForDataServerAndLabelsAndConfigs(
            labels, processName, lang, skipPresent, returnStatistics, configs);
    return addStatusToPipelineResponse(pre, processName);
  }

  private PipelineResponse addStatusToPipelineResponse(
      PipelineResponseEntity responseEntity, String processName) {
    if (responseEntity == null) {
      return new PipelineResponse()
          .name(processName)
          .response("Pipeline failed. Please consult the logs.")
          .status(PipelineResponseStatus.FAILED);
    } else if (responseEntity instanceof PipelineFailEntity) {
      return responseEntity.getSpecificResponse().status(PipelineResponseStatus.FAILED);
    }
    return responseEntity.getSpecificResponse().status(PipelineResponseStatus.SUCCESSFUL);
  }
}
