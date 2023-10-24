package care.smith.top.backend.service.nlp;

import care.smith.top.model.ConceptGraph;
import care.smith.top.model.ConceptGraphProcess;
import care.smith.top.model.ConceptGraphStat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@SpringBootTest
class ConceptGraphsServiceTest extends AbstractNLPTest{
  @Autowired ConceptGraphsService conceptGraphsService;

  //ToDo: build test zip here to use

  @Test
  void getAllConceptGraphStatistics() {
    Map<String, ConceptGraphStat> conceptGraphStatMap = conceptGraphsService.getAllConceptGraphStatistics("grassco");
  }

  @Test
  void getConceptGraphForIdAndProcess() {
    ConceptGraph conceptGraph = conceptGraphsService.getConceptGraphForIdAndProcess("0", "grassco");
  }

  @Test
  void getStoredProcesses() {
    List<ConceptGraphProcess> processes = conceptGraphsService.getAllStoredProcesses();
  }

  @Test
  void initPipeline() {
    try {
      Map<String, ConceptGraphStat> stats =
          conceptGraphsService.initPipeline(
              new File(Objects.requireNonNull(getClass().getClassLoader().getResource("test_files.zip")).toURI()),
              "grassco");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
