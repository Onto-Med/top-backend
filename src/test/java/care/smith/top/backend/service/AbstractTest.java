package care.smith.top.backend.service;

import care.smith.top.backend.api.OrganisationApiDelegateImpl;
import care.smith.top.backend.repository.EntityRepository;
import care.smith.top.backend.repository.OrganisationRepository;
import care.smith.top.backend.repository.RepositoryRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractTest {
  @Autowired OrganisationApiDelegateImpl organisationApiDelegate;
  @Autowired OrganisationService organisationService;
  @Autowired OrganisationRepository organisationRepository;
  @Autowired RepositoryService repositoryService;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired EntityService entityService;
  @Autowired EntityRepository entityRepository;
}
