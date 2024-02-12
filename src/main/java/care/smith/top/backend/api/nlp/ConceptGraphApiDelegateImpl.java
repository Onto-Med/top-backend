package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.ConceptgraphsApiDelegate;
import care.smith.top.backend.service.nlp.ConceptClusterService;
import care.smith.top.backend.service.nlp.ConceptGraphsService;
import care.smith.top.model.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConceptGraphApiDelegateImpl implements ConceptgraphsApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(ConceptGraphApiDelegateImpl.class.getName());
  private final ConceptGraphsService conceptGraphsService;

  public ConceptGraphApiDelegateImpl(
      ConceptGraphsService conceptGraphsService, ConceptClusterService conceptClusterService) {
    this.conceptGraphsService = conceptGraphsService;
  }

  @Override
  public ResponseEntity<Map<String, ConceptGraphStat>> getConceptGraphStatistics(
      List<String> include, String process) {
    Map<String, ConceptGraphStat> statistics =
        conceptGraphsService.getAllConceptGraphStatistics(process);
    if (statistics == null) return ResponseEntity.of(Optional.empty());
    return ResponseEntity.ok(statistics);
  }

  @Override
  public ResponseEntity<ConceptGraph> getConceptGraph(
      String processId, String graphId, List<String> include) {
    return ResponseEntity.ok(
        conceptGraphsService.getConceptGraphForIdAndProcess(graphId, processId));
  }

  @Override
  public ResponseEntity<List<ConceptGraphProcess>> getStoredProcesses(List<String> include) {
    return ResponseEntity.ok(conceptGraphsService.getAllStoredProcesses());
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipelineWithoutUpload(
      String process,
      List<String> include,
      String lang,
      Boolean skipPresent,
      Boolean returnStatistics) {
    return startConceptGraphPipeline(
        process,
        include,
        lang,
        skipPresent,
        returnStatistics,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Override
  public ResponseEntity<PipelineResponse> startConceptGraphPipeline(
      String process,
      List<String> include,
      String lang,
      Boolean skipPresent,
      Boolean returnStatistics,
      MultipartFile data,
      MultipartFile labels,
      MultipartFile dataConfig,
      MultipartFile embeddingConfig,
      MultipartFile clusteringConfig,
      MultipartFile graphConfig,
      MultipartFile documentServerConfig) {
    if (data == null
        && (documentServerConfig == null
            && conceptGraphsService.getDocumentServerAddress() == null)) {
      return ResponseEntity.badRequest()
          .body(
              new PipelineResponse()
                  .name(process != null ? process : "default")
                  .response(
                      "Neither 'data' nor configuration for a document server ('documentServerConfig') were provided. "
                          + "There also seems no default document server to be available. One of either is needed.")
                  .status(PipelineResponseStatus.FAILED));
    }
    Map<String, File> configMap =
        Stream.of(
                Pair.of("data", dataConfig),
                Pair.of("embedding", embeddingConfig),
                Pair.of("clustering", clusteringConfig),
                Pair.of("graph", graphConfig))
            .filter(pair -> pair.getRight() != null)
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      try {
                        return e.getValue().getResource().getFile();
                      } catch (IOException ex) {
                        throw new RuntimeException(ex);
                      }
                    }));

    if (documentServerConfig != null) {
      try {
        configMap.put("document_server", documentServerConfig.getResource().getFile());
      } catch (IOException e) {
        LOGGER.severe(
            "Couldn't access document_server_config file. Something went wrong with the upload.");
        throw new RuntimeException(e);
      }
    } else if (data == null && conceptGraphsService.getDocumentServerAddress() != null) {
      try {
        configMap.put("document_server", createTemporaryDataServerConfig());
      } catch (IOException e) {
        LOGGER.severe("Couldn't create temporary document_server_config file.");
        throw new RuntimeException(e);
      }
    }

    try {
      PipelineResponse pipelineResponse;
      if (data != null) {
        pipelineResponse =
            conceptGraphsService.initPipelineWithDataUploadAndWithConfigs(
                data.getResource().getFile(),
                labels != null ? labels.getResource().getFile() : null,
                process,
                lang,
                skipPresent,
                returnStatistics,
                configMap);
      } else {
        pipelineResponse =
            conceptGraphsService.initPipelineWithDataServerAndWithConfigs(
                labels != null ? labels.getResource().getFile() : null,
                process,
                lang,
                skipPresent,
                returnStatistics,
                configMap);
      }
      if (pipelineResponse.getStatus().equals(PipelineResponseStatus.FAILED)) {
        return ResponseEntity.of(Optional.of(pipelineResponse));
      }
      return ResponseEntity.ok(pipelineResponse);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File createTemporaryDataServerConfig() throws IOException {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    File tmpFile = new File(tmpDir.getAbsolutePath(), "document_server_config.yaml");
    if (!tmpFile.exists()) {
      tmpDir.mkdirs();
    } else {
      tmpFile.delete();
    }
    File dataServerConfig = new File(tmpFile.getAbsolutePath());
    FileWriter fileWriter = new FileWriter(dataServerConfig);
    PrintWriter printWriter = new PrintWriter(fileWriter);
    int lastIndexOfColon = conceptGraphsService.getDocumentServerAddress().lastIndexOf(":");
    printWriter.printf(
        "url: %s\n",
        conceptGraphsService.getDocumentServerAddress().substring(0, lastIndexOfColon));
    printWriter.printf(
        "port: %s\n",
        conceptGraphsService.getDocumentServerAddress().substring(lastIndexOfColon + 1));
    printWriter.printf("index: %s\n", conceptGraphsService.getDocumentServerIndexName());
    printWriter.printf("size: %s\n", conceptGraphsService.getDocumentServerBatchSize());
    printWriter.printf(
        "replace_keys: %s\n", conceptGraphsService.getDocumentServerFieldsReplacement());
    printWriter.close();
    return dataServerConfig;
  }
}
