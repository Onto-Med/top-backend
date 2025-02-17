package care.smith.top.backend.service.nlp;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.AbstractTest;
import care.smith.top.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DocumentQueryServiceTest extends AbstractTest {

  @Test
  void subDependencyGathering() {
    //    Organisation organisation =
    //        organisationService.createOrganisation(new Organisation().id("org"));
    //    Repository repository =
    //        repositoryService.createRepository(
    //            organisation.getId(),
    //            new Repository()
    //                .id("repo")
    //                .organisation(organisation)
    //                .repositoryType(RepositoryType.CONCEPT_REPOSITORY),
    //            null);
    //
    //    SingleConcept singleConcept =
    //        (SingleConcept)
    //            new SingleConcept()
    //                .titles(List.of(new LocalisableText().text("Single Concept 1").lang("en")))
    //                .entityType(EntityType.SINGLE_CONCEPT)
    //                .id("sing_con");
    //    SingleConcept subConcept1 =
    //        (SingleConcept)
    //            new SingleConcept()
    //                .superConcepts(List.of(singleConcept))
    //                .entityType(EntityType.SINGLE_CONCEPT)
    //                .id("sub_con_1");
    //    SingleConcept subConcept2 =
    //        (SingleConcept)
    //            new SingleConcept()
    //                .superConcepts(List.of(singleConcept))
    //                .entityType(EntityType.SINGLE_CONCEPT)
    //                .id("sub_con_2");
    //    SingleConcept subConcept3 =
    //        (SingleConcept)
    //            new SingleConcept()
    //                .superConcepts(List.of(subConcept1))
    //                .entityType(EntityType.SINGLE_CONCEPT)
    //                .id("sub_con_3");
    //    SingleConcept subConcept4 =
    //        (SingleConcept)
    //            new SingleConcept()
    //                .superConcepts(List.of(subConcept3))
    //                .entityType(EntityType.SINGLE_CONCEPT)
    //                .id("sub_con_4");
    //
    //    List<Entity> bulk =
    //        Arrays.asList(singleConcept, subConcept1, subConcept2, subConcept3, subConcept4);
    //
    //    assertThatCode(
    //            () ->
    //                entityService.createEntities(organisation.getId(), repository.getId(), bulk,
    // null))
    //        .doesNotThrowAnyException();
    //    assertThat(entityService.count()).isEqualTo(bulk.size());
    //    Map<String, Set<String>> subDependencies = new HashMap<>();
    //    Map<String, Entity> conceptMap = Map.of("sing_con", singleConcept);
    //    Map<String, Integer> depthMap = Map.of("sing_con", 2);
    //    conceptRepository.populateEntities(conceptMap, subDependencies, depthMap);
    //        assertThat(conceptRepository.getEntityTreeByEntityId("sing_con").size()).isEqualTo(3);
    // // , Integer.toString(1)
  }
}
