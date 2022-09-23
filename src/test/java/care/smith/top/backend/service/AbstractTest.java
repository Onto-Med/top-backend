package care.smith.top.backend.service;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractTest {
  @Autowired OrganisationApiDelegateImpl organisationApiDelegate;
  @Autowired OrganisationService organisationService;
  @Autowired OrganisationRepository organisationRepository;
  @Autowired RepositoryService repositoryService;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired EntityService entityService;
  @Autowired CategoryRepository categoryRepository;
  @Autowired EntityRepository entityRepository;
  @Autowired PhenotypeRepository phenotypeRepository;
}
