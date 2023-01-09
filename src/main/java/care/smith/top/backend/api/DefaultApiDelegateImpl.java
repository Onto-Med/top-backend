package care.smith.top.backend.api;

import care.smith.top.backend.service.nlp.DocumentService;
import care.smith.top.model.EntityType;
import care.smith.top.model.Format;
import care.smith.top.model.Purpose;
import care.smith.top.model.Statistic;
import care.smith.top.backend.service.EntityService;
import care.smith.top.backend.service.OrganisationService;
import care.smith.top.backend.service.RepositoryService;
import care.smith.top.top_phenotypic_query.converter.PhenotypeExporter;
import care.smith.top.top_phenotypic_query.converter.PhenotypeImporter;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.SubTypes;

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
  public ResponseEntity<List<Format>> getFormats(Purpose purpose) {
    Reflections reflections = new Reflections("care.smith.top");
    List<Format> formats =
        reflections
            .get(SubTypes.of(PhenotypeImporter.class, PhenotypeExporter.class).asClass())
            .stream()
            .map(
                c -> {
                  Format format = new Format().id(c.getSimpleName()).purposes(new ArrayList<>());
                  if (PhenotypeImporter.class.isAssignableFrom(c))
                    format.addPurposesItem(Purpose.IMPORT);
                  if (PhenotypeExporter.class.isAssignableFrom(c))
                    format.addPurposesItem(Purpose.EXPORT);
                  return format;
                })
            .filter(f -> purpose == null || f.getPurposes().contains(purpose))
            .collect(Collectors.toList());
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
