package care.smith.top.backend.api;

import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.*;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DefaultApiDelegateImpl implements DefaultApiDelegate {
  private final Logger LOGGER = Logger.getLogger(DefaultApiDelegateImpl.class.getName());

  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;

  @Autowired DocumentService documentService;

  @Value("${top.appName:TOP Backend}")
  String appName;

  @Value("${top.version:unknown}")
  String version;

  @Override
  public ResponseEntity<AppDescription> ping() {
    return ResponseEntity.ok(new AppDescription().appName(appName).version(version));
  }

  @Override
  public ResponseEntity<Statistic> getStatistics() {
    long documentCount = 0;
    try {
      documentCount = documentService.count();
    } catch (Throwable e) {
      LOGGER.warning(e.getMessage());
    }
    Statistic statistic =
        new Statistic()
            .organisations(organisationService.count())
            .repositories(repositoryService.count())
            .categories(entityService.count(EntityType.CATEGORY))
            .phenotypes(
                entityService.count(
                    EntityType.SINGLE_PHENOTYPE,
                    EntityType.SINGLE_RESTRICTION,
                    EntityType.COMPOSITE_PHENOTYPE,
                    EntityType.COMPOSITE_RESTRICTION))
            .documents(documentCount);
    return new ResponseEntity<>(statistic, HttpStatus.OK);
  }
}
