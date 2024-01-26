package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.conceptgraphs.ConceptGraphStatisticsEntity;
import care.smith.top.backend.model.conceptgraphs.GraphStatsEntity;
import care.smith.top.backend.model.conceptgraphs.PipelineFailEntity;
import care.smith.top.backend.model.conceptgraphs.PipelineResponseEntity;
import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.*;

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
  @Value("${top.documents.document_server.batch_size}")
  private Integer documentServerBatchSize;
  @Value("${top.documents.document_server.fields_replacement}")
  private String documentServerFieldsReplacement;

  private final ConceptGraphsRepository conceptGraphsRepository;

  public ConceptGraphsService(ConceptGraphsRepository conceptGraphsRepository) {
    this.conceptGraphsRepository = conceptGraphsRepository;
  }

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
    return Arrays.stream(conceptGraphsRepository.getAllStoredProcesses().getProcesses()).count();
  }

  public Map<String, ConceptGraphStat> getAllConceptGraphStatistics(String processName) {
    ConceptGraphStatisticsEntity conceptGraphStatisticsEntity =
        conceptGraphsRepository.getGraphStatisticsForProcess(processName);
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
    PipelineResponseEntity pre =  conceptGraphsRepository
        .startPipelineForData(data, processName, null, true, false);
    return addStatusToPipelineResponse(pre, processName);
  }

  public PipelineResponse initPipelineWithBooleans(
      File data, String processName, Boolean skipPresent, Boolean returnStatistics) {
    PipelineResponseEntity pre = conceptGraphsRepository
        .startPipelineForData(data, processName, null, skipPresent, returnStatistics);
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
     PipelineResponseEntity pre = conceptGraphsRepository
        .startPipelineForDataAndLabelsAndConfigs(
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
    PipelineResponseEntity pre = conceptGraphsRepository
        .startPipelineForDataServerAndLabelsAndConfigs(
            labels, processName, lang, skipPresent, returnStatistics, configs);
    return addStatusToPipelineResponse(pre, processName);
  }

  private PipelineResponse addStatusToPipelineResponse(PipelineResponseEntity responseEntity, String processName) {
    if (responseEntity == null) {
      return new PipelineResponse()
          .name(processName)
          .response("Pipeline failed. Please consult the logs.")
          .status(PipelineResponseStatus.FAILED);
    } else if (responseEntity instanceof PipelineFailEntity) {
      //ToDo: this needs to get more meaningful -> ConceptGraphsRepository.callApi should account for more HttpResponse Codes
      return responseEntity.getSpecificResponse().status(PipelineResponseStatus.FAILED);
    }
    return responseEntity.getSpecificResponse().status(PipelineResponseStatus.SUCCESSFUL);
  }
}
