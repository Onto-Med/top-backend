package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.conceptgraphs.ConceptGraphsRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.ConceptGraphStat;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ConceptGraphsService implements ContentService {

  private final ConceptGraphsRepository conceptGraphsRepository;

  public ConceptGraphsService(ConceptGraphsRepository conceptGraphsRepository) {
    this.conceptGraphsRepository = conceptGraphsRepository;
  }

  @Override
  public long count() {
    return 0;
  }

  public void getAllConceptGraphStatistics(String processName) {
    Map<String, ConceptGraphStat> map = conceptGraphsRepository.getGraphStatisticsForProcess(processName);
    System.out.println();
  }
}
