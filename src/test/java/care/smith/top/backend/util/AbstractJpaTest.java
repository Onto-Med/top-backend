package care.smith.top.backend.util;

import care.smith.top.backend.TopBackendApplication;
import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.jpa.CategoryRepository;
import care.smith.top.backend.repository.jpa.ConceptRepository;
import care.smith.top.backend.repository.jpa.EntityRepository;
import care.smith.top.backend.repository.jpa.EntityVersionRepository;
import care.smith.top.backend.repository.jpa.OrganisationMembershipRepository;
import care.smith.top.backend.repository.jpa.OrganisationRepository;
import care.smith.top.backend.repository.jpa.PhenotypeRepository;
import care.smith.top.backend.repository.jpa.RepositoryRepository;
import care.smith.top.backend.repository.jpa.UserRepository;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ExtendWith(PostgreSqlTestcontainersInitializer.class)
@ContextConfiguration(
    classes = TopBackendApplication.class,
    initializers = PostgreSqlTestcontainersInitializer.class)
public class AbstractJpaTest {
  @Autowired protected OrganisationApiDelegateImpl organisationApiDelegate;
  @Autowired protected OrganisationService organisationService;
  @Autowired protected OrganisationRepository organisationRepository;
  @Autowired protected RepositoryService repositoryService;
  @Autowired protected RepositoryRepository repositoryRepository;
  @Autowired protected EntityService entityService;
  @Autowired protected CategoryRepository categoryRepository;
  @Autowired protected ConceptRepository conceptRepository;
  @Autowired protected EntityRepository entityRepository;
  @Autowired protected PhenotypeRepository phenotypeRepository;
  @Autowired protected EntityVersionRepository entityVersionRepository;
  @Autowired protected UserRepository userRepository;
  @Autowired protected UserService userService;
  @Autowired protected OrganisationMembershipRepository organisationMembershipRepository;
  @Autowired protected SubjectRepository subjectRepository;
  @Autowired protected EncounterRepository encounterRepository;
  @Autowired protected SubjectResourceRepository subjectResourceRepository;

  @BeforeEach
  public void resetState() {
    organisationRepository.deleteAll();
    userRepository.deleteAll();
    subjectRepository.deleteAll();
    encounterRepository.deleteAll();
    subjectResourceRepository.deleteAll();
  }
}
