package care.smith.top.backend.api;

import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.converter.PhenotypeExporter;
import care.smith.top.top_phenotypic_query.converter.PhenotypeImporter;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
  public ResponseEntity<List<Converter>> getConverters(Purpose purpose) {
    Reflections reflections = new Reflections("care.smith.top");
    List<Converter> formats = new ArrayList<>();

    if (purpose == null || purpose.equals(Purpose.IMPORT)) {
      formats.addAll(
          reflections.getSubTypesOf(PhenotypeImporter.class).stream()
              .map(
                  c -> {
                    Converter format =
                        new Converter().id(c.getSimpleName()).purpose(Purpose.IMPORT);
                    try {
                      PhenotypeImporter instance = c.getConstructor().newInstance();
                      format.setFileExtension(instance.getFileExtension());
                    } catch (Exception ignored) {
                    }
                    return format;
                  })
              .collect(Collectors.toList()));
    }

    if (purpose == null || purpose.equals(Purpose.EXPORT)) {
      formats.addAll(
          reflections.getSubTypesOf(PhenotypeExporter.class).stream()
              .map(
                  c -> {
                    Converter format =
                        new Converter().id(c.getSimpleName()).purpose(Purpose.EXPORT);
                    try {
                      PhenotypeExporter instance = c.getConstructor().newInstance();
                      format.setFileExtension(instance.getFileExtension());
                    } catch (Exception ignored) {
                    }
                    return format;
                  })
              .collect(Collectors.toList()));
    }

    formats.sort(Comparator.comparing(Converter::getId));
    return new ResponseEntity<>(formats, HttpStatus.OK);
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
