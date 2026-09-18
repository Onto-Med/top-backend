package care.smith.top.backend.service.nlp;

import care.smith.top.top_document_query.concept_graphs_api.QueryExpansionManager;
import care.smith.top.top_document_query.concept_graphs_api.model.QueryExpansionProfileEntity;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QueryExpansionService {
  private final QueryExpansionManager queryExpansionManager;
  private final boolean cgApiEnabled;

  public QueryExpansionService(
      @Value("${top.documents.concept-graphs-api.uri}") String conceptGraphsApiUri,
      @Value("${top.documents.concept-graphs-api.enabled}") boolean cgApiEnabled) {
    this.cgApiEnabled = cgApiEnabled;
    QueryExpansionManager tmpManager;
    try {
      tmpManager = new QueryExpansionManager(conceptGraphsApiUri);
    } catch (MalformedURLException | URISyntaxException e) {
      try {
        tmpManager = new QueryExpansionManager("http://localhost:9010");
      } catch (MalformedURLException | URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
    }
    queryExpansionManager = tmpManager;
  }

  public Optional<QueryExpansionProfileEntity> getProfile(String profileName) {
    if (!cgApiEnabled) return Optional.empty();
    return queryExpansionManager.getProfile(profileName);
  }
}
