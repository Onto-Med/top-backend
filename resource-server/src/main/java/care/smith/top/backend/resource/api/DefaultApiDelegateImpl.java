package care.smith.top.backend.resource.api;

import care.smith.top.backend.api.DefaultApiDelegate;
import care.smith.top.backend.model.EntityType;
import care.smith.top.backend.model.Statistic;
import care.smith.top.backend.resource.service.EntityService;
import care.smith.top.backend.resource.service.OrganisationService;
import care.smith.top.backend.resource.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DefaultApiDelegateImpl implements DefaultApiDelegate {
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;

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
                    EntityType.COMPOSITE_RESTRICTION));
    return new ResponseEntity<>(statistic, HttpStatus.OK);
  }
}
