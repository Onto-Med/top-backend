package care.smith.top.backend.util;

import care.smith.top.backend.configuration.AuthenticatedUser;
import care.smith.top.backend.model.jpa.UserDao;
import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.UserService;
import care.smith.top.model.Organisation;
import care.smith.top.model.Repository;
import care.smith.top.model.RepositoryType;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataImporter {
  private final Logger LOGGER = Logger.getLogger(DemoDataImporter.class.getName());
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;
  @Autowired UserService userService;

  @Value("${top.demo-data:false}")
  private boolean importDemoData;

  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    if (!importDemoData) return;
    LOGGER.fine("Importing demo data...");
    try {
      importData();
    } catch (Exception e) {
      LOGGER.fine("Demo data import failed! " + e.getMessage());
    }
  }

  @Transactional
  private void importData() {
    UserDao demoUser = buildDemoUser();
    SecurityContextHolder.getContext().setAuthentication(new AuthenticatedUser(demoUser));

    Organisation demoOrganisation =
        new Organisation()
            .id("demo-organisation")
            .name("Demo Organisation")
            .description("This is a demo organisation with a sample repository to get started!");
    organisationService.createOrganisation(demoOrganisation);
    LOGGER.finer("Demo organisation imported.");

    Repository demoRepository =
        new Repository()
            .id("demo-repository")
            .organisation(demoOrganisation)
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY)
            .primary(true)
            .name("Demo Repository")
            .description(
                "This repository contains a simple phenotype model for reasoning about BMI.");
    repositoryService.createRepository(demoOrganisation.getId(), demoRepository, null);
    LOGGER.finer("Demo repository imported.");

    entityService.importRepository(
        demoOrganisation.getId(),
        demoRepository.getId(),
        TopJsonFormat.class.getSimpleName(),
        DemoDataImporter.class.getResourceAsStream("/demo-data.json"));
    LOGGER.finer("Demo phenotypes imported.");
  }

  private UserDao buildDemoUser() {
    Jwt jwt =
        Jwt.withTokenValue("dummy-token")
            .claim("sub", "demo-user")
            .claim("name", "Demo User")
            .header("Accept", "application/json")
            .build();
    return userService.getOrCreateUser(jwt).orElse(null);
  }
}
