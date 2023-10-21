package care.smith.top.backend.service.nlp;

import care.smith.top.model.ConceptGraphProcess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class ConceptGraphsServiceTest extends AbstractNLPTest{
  @Autowired ConceptGraphsService conceptGraphsService;

  @Test
  void getAllConceptGraphStatistics() {
    conceptGraphsService.getAllConceptGraphStatistics("grassco");
  }

  @Test
  void getConceptGraphForIdAndProcess() {
    conceptGraphsService.getConceptGraphForIdAndProcess("0", "grassco");
  }

  @Test
  void getStoredProcesses() {
    List<ConceptGraphProcess> processes = conceptGraphsService.getAllStoredProcesses();
  }
}
