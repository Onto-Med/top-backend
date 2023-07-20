package care.smith.top.backend.service;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class AbstractTest {
  @Autowired OrganisationApiDelegateImpl organisationApiDelegate;
  @Autowired OrganisationService organisationService;
  @Autowired OrganisationRepository organisationRepository;
  @Autowired RepositoryService repositoryService;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired EntityService entityService;
  @Autowired CategoryRepository categoryRepository;
  @Autowired ConceptRepository conceptRepository;
  @Autowired EntityRepository entityRepository;
  @Autowired PhenotypeRepository phenotypeRepository;
  @Autowired EntityVersionRepository entityVersionRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Autowired OrganisationMembershipRepository organisationMembershipRepository;

  @BeforeEach
  public void resetState() {
    organisationRepository.deleteAll();
    userRepository.deleteAll();
  }
}
