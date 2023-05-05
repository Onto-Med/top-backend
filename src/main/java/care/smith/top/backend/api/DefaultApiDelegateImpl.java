package care.smith.top.backend.api;

import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DefaultApiDelegateImpl implements DefaultApiDelegate {
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;

  @Autowired DocumentService documentService;

  @Override
  public ResponseEntity<Void> ping() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Statistic> getStatistics() {
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
            .documents(documentService.count());
    return new ResponseEntity<>(statistic, HttpStatus.OK);
  }
}
