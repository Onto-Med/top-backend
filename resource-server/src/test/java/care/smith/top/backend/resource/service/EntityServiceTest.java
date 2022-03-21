package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.*;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.repository.ClassRepository;
import care.smith.top.backend.neo4j_ontology_access.repository.ClassVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EntityServiceTest extends Neo4jTest {
  @Autowired OrganisationService organisationService;
  @Autowired RepositoryService repositoryService;
  @Autowired EntityService entityService;
  @Autowired ClassRepository classRepository;
  @Autowired ClassVersionRepository classVersionRepository;

  @Test
  void createEntity() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(organisation.getId(), new Repository().id("repo"), null);

    Repository codeRepository =
        repositoryService.createRepository(
            organisation.getId(), new Repository().id("http://loinc.org"), null);

    // TODO: use codeRepository service, when available
    Class code = classRepository.save(new Class("1234").setRepositoryId(codeRepository.getId()));

    assertThat(code)
        .isNotNull()
        .satisfies(
            c -> {
              assertThat(c.getId()).isEqualTo("1234");
              assertThat(c.getRepositoryId()).isEqualTo(codeRepository.getId());
            });

    /* Create category */
    Category category = new Category();
    category
        .id(UUID.randomUUID().toString())
        .entityType(EntityType.CATEGORY)
        .addTitlesItem(new LocalisableText().text("Category").lang("en"))
        .addDescriptionsItem(new LocalisableText().text("Some description").lang("en"))
        .addSynonymsItem(new LocalisableText().text("Some synonym").lang("en"))
        .addCodesItem(
            new Code()
                .code(code.getId())
                .codeSystem(new CodeSystem().uri(URI.create(codeRepository.getId()))));

    assertThatThrownBy(
            () -> entityService.createEntity("does not exist", repository.getId(), category))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatThrownBy(
            () -> entityService.createEntity(organisation.getId(), "does not exist", category))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(entityService.createEntity(organisation.getId(), repository.getId(), category))
        .isNotNull()
        .isInstanceOf(Category.class)
        .satisfies(
            c -> {
              assertThat(c.getId()).isEqualTo(category.getId());
              assertThat(c.getEntityType()).isEqualTo(category.getEntityType());
              assertThat(c.getDescriptions()).isEqualTo(category.getDescriptions());
              assertThat(c.getEquivalentEntities()).isNullOrEmpty();
              assertThat(c.getCodes()).isNotEmpty();
              assertThat(c.getCreatedAt()).isNotNull();
              assertThat(c.getSynonyms()).isEqualTo(category.getSynonyms());
              assertThat(c.getVersion()).isEqualTo(1);
              assertThat(c.getRepository())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", repository.getId());
              assertThat(((Category) c).getSuperCategories()).isNullOrEmpty();
            });

    assertThatThrownBy(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), category))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    /* Create abstract phenotype */
    Phenotype abstractPhenotype = new Phenotype().addUnitsItem(new Unit().unit("cm"));
    abstractPhenotype
        .expression(
            new Expression()
                .operator("complement")
                .addOperandsItem(new Expression().operator("entity")))
        .addSuperCategoriesItem(category)
        .index(5)
        .id(UUID.randomUUID().toString())
        .entityType(EntityType.SINGLE_PHENOTYPE);

    assertThat(
            entityService.createEntity(organisation.getId(), repository.getId(), abstractPhenotype))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(
            p -> {
              assertThat(p.getId()).isEqualTo(abstractPhenotype.getId());
              assertThat(p.getVersion()).isEqualTo(1);
              assertThat(p.getEntityType()).isEqualTo(abstractPhenotype.getEntityType());
              assertThat(((Phenotype) p).getSuperPhenotype()).isNull();
              assertThat(((Phenotype) p).getSuperCategories())
                  .allMatch(c -> c.getId().equals(category.getId()));
              assertThat(p.getIndex()).isEqualTo(5);
              assertThat(((Phenotype) p).getUnits()).allMatch(u -> u.getUnit().equals("cm"));
              assertThat(((Phenotype) p).getExpression())
                  .isNotNull()
                  .satisfies(
                      e -> {
                        assertThat(e.getOperator())
                            .isEqualTo(abstractPhenotype.getExpression().getOperator());
                        assertThat(e.getOperands()).size().isEqualTo(1);
                      });
            });

    /* Create restricted phenotype */
    Phenotype restrictedPhenotype1 =
        new Phenotype()
            .score(BigDecimal.valueOf(10))
            .restriction(
                new NumberRestriction()
                    .addValuesItem(BigDecimal.valueOf(50))
                    .minOperator(RestrictionOperator.GREATER_THAN)
                    .quantor(Quantor.SOME)
                    .type(DataType.NUMBER));
    restrictedPhenotype1
        .superPhenotype(abstractPhenotype)
        .id(UUID.randomUUID().toString())
        .entityType(EntityType.SINGLE_RESTRICTION)
        .addTitlesItem(new LocalisableText().text("> 50cm").lang("en"));

    assertThat(
            entityService.createEntity(
                organisation.getId(), repository.getId(), restrictedPhenotype1))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(
            rp -> {
              assertThat(rp.getId()).isEqualTo(restrictedPhenotype1.getId());
              assertThat(rp.getVersion()).isEqualTo(1);
              assertThat(rp.getEntityType()).isEqualTo(EntityType.SINGLE_RESTRICTION);
              assertThat(((Phenotype) rp).getSuperPhenotype())
                  .hasFieldOrPropertyWithValue("id", abstractPhenotype.getId());
              assertThat(rp.getTitles())
                  .allMatch(t -> t.getText().equals("> 50cm") && t.getLang().equals("en"))
                  .size()
                  .isEqualTo(1);
              assertThat(rp.getRepository()).hasFieldOrPropertyWithValue("id", repository.getId());
              assertThat(((Phenotype) rp).getSuperCategories()).isNullOrEmpty();
              assertThat(((Phenotype) rp).getScore().compareTo(BigDecimal.valueOf(10)))
                  .isEqualTo(0);
              assertThat(((Phenotype) rp).getRestriction())
                  .isNotNull()
                  .isInstanceOf(NumberRestriction.class)
                  .satisfies(
                      r -> {
                        assertThat(r.getQuantor()).isEqualTo(Quantor.SOME);
                        assertThat(((NumberRestriction) r).getMaxOperator()).isNull();
                        assertThat(((NumberRestriction) r).getMinOperator())
                            .isNotNull()
                            .isEqualTo(RestrictionOperator.GREATER_THAN);
                        assertThat(((NumberRestriction) r).getValues())
                            .allMatch(v -> v.compareTo(BigDecimal.valueOf(50)) == 0)
                            .size()
                            .isEqualTo(1);
                      });
            });

    Phenotype restrictedPhenotype2 =
        new Phenotype()
            .score(BigDecimal.valueOf(-5))
            .restriction(
                new NumberRestriction()
                    .addValuesItem(BigDecimal.valueOf(50))
                    .maxOperator(RestrictionOperator.LESS_THAN_OR_EQUAL_TO)
                    .quantor(Quantor.ALL)
                    .type(DataType.NUMBER));
    restrictedPhenotype2
        .superPhenotype(abstractPhenotype)
        .id(UUID.randomUUID().toString())
        .entityType(EntityType.SINGLE_RESTRICTION)
        .addTitlesItem(new LocalisableText().text("<= 50cm").lang("en"));

    assertThat(
            entityService.createEntity(
                organisation.getId(), repository.getId(), restrictedPhenotype2))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(
            rp -> {
              assertThat(((Phenotype) rp).getSuperPhenotype())
                  .hasFieldOrPropertyWithValue("id", abstractPhenotype.getId());
              assertThat(rp.getTitles())
                  .allMatch(t -> t.getText().equals("<= 50cm") && t.getLang().equals("en"))
                  .size()
                  .isEqualTo(1);
              assertThat(((Phenotype) rp).getScore().compareTo(BigDecimal.valueOf(-5)))
                  .isEqualTo(0);
              assertThat(((Phenotype) rp).getRestriction())
                  .isNotNull()
                  .isInstanceOf(NumberRestriction.class)
                  .satisfies(
                      r -> {
                        assertThat(r.getQuantor()).isEqualTo(Quantor.ALL);
                        assertThat(((NumberRestriction) r).getMinOperator()).isNull();
                        assertThat(((NumberRestriction) r).getMaxOperator())
                            .isNotNull()
                            .isEqualTo(RestrictionOperator.LESS_THAN_OR_EQUAL_TO);
                        assertThat(((NumberRestriction) r).getValues())
                            .allMatch(v -> v.compareTo(BigDecimal.valueOf(50)) == 0)
                            .size()
                            .isEqualTo(1);
                      });
            });

    assertThat(
            classRepository.findByIdAndRepositoryId(abstractPhenotype.getId(), repository.getId()))
        .isPresent();
    assertThat(classRepository.findSubclasses(abstractPhenotype.getId(), repository.getId()))
        .isNotEmpty()
        .size()
        .isEqualTo(2);

    assertThat(entityService.getRestrictions(repository.getId(), abstractPhenotype))
        .isNotEmpty()
        .size()
        .isEqualTo(2);
  }

  @Test
  void deleteVersion() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(organisation.getId(), new Repository().id("repo"), null);
    Phenotype phenotype =
        (Phenotype)
            new Phenotype()
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.SINGLE_PHENOTYPE);

    assertThat(entityService.createEntity(organisation.getId(), repository.getId(), phenotype))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(1));

    assertThat(
            entityService.updateEntityById(
                organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(2));

    assertThat(
            entityService.updateEntityById(
                organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(3));

    assertThatThrownBy(
            () ->
                entityService.deleteVersion(
                    organisation.getId(), repository.getId(), phenotype.getId(), 4))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThat(classVersionRepository.findCurrentByClassId(phenotype.getId()))
        .isPresent()
        .hasValueSatisfying(
            cv -> {
              assertThat(cv.getaClass()).isNotNull();
              assertThat(cv.getVersion()).isEqualTo(3);
            });

    // Delete
    assertThatCode(
            () ->
                entityService.deleteVersion(
                    organisation.getId(), repository.getId(), phenotype.getId(), 2))
        .doesNotThrowAnyException();

    assertThat(classVersionRepository.findCurrentByClassId(phenotype.getId()))
        .isPresent()
        .hasValueSatisfying(
            cv -> {
              assertThat(cv.getaClass()).isNotNull();
              assertThat(cv.getVersion()).isEqualTo(3);
              assertThat(classVersionRepository.getPrevious(cv))
                  .isPresent()
                  .hasValueSatisfying(prev -> assertThat(prev.getVersion()).isEqualTo(1));
            });

    assertThat(
            entityService.loadEntity(
                organisation.getId(), repository.getId(), phenotype.getId(), null))
        .isNotNull()
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(3));

    assertThat(
            entityService.loadEntity(
                organisation.getId(), repository.getId(), phenotype.getId(), 1))
        .isNotNull()
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(1));

    assertThatThrownBy(
            () ->
                entityService.loadEntity(
                    organisation.getId(), repository.getId(), phenotype.getId(), 2))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void deleteEntity() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(organisation.getId(), new Repository().id("repo"), null);
    Phenotype phenotype =
        (Phenotype)
            new Phenotype()
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.SINGLE_PHENOTYPE);

    assertThat(entityService.createEntity(organisation.getId(), repository.getId(), phenotype))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(1));

    assertThat(
            entityService.updateEntityById(
                organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(p -> assertThat(p.getVersion()).isEqualTo(2));

    assertThatThrownBy(
            () ->
                entityService.deleteEntity(organisation.getId(), repository.getId(), "invalid id"))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatCode(
            () ->
                entityService.deleteEntity(
                    organisation.getId(), repository.getId(), phenotype.getId()))
        .doesNotThrowAnyException();

    assertThatThrownBy(
            () ->
                entityService.loadEntity(
                    organisation.getId(), repository.getId(), phenotype.getId(), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void getEntities() {}

  @Test
  void getSubclasses() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(organisation.getId(), new Repository().id("repo"), null);

    Category superCat = (Category) new Category().entityType(EntityType.CATEGORY).id("super_cat");
    Category subCat1 =
        (Category)
            new Category()
                .superCategories(Collections.singletonList(superCat))
                .entityType(EntityType.CATEGORY)
                .id("sub_cat_1");
    Category subCat2 =
        (Category)
            new Category()
                .superCategories(Collections.singletonList(superCat))
                .entityType(EntityType.CATEGORY)
                .id("sub_cat_2");
    Category subCat3 =
        (Category)
            new Category()
                .superCategories(Collections.singletonList(subCat1))
                .entityType(EntityType.CATEGORY)
                .id("sub_cat_3");

    assertThatCode(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), superCat))
        .doesNotThrowAnyException();
    assertThatCode(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), subCat1))
        .doesNotThrowAnyException();
    assertThatCode(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), subCat2))
        .doesNotThrowAnyException();
    assertThatCode(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), subCat3))
        .doesNotThrowAnyException();

    assertThat(
            entityService.getSubclasses(
                organisation.getId(), repository.getId(), superCat.getId(), null))
        .isNotEmpty()
        .anySatisfy((sub) -> assertThat(sub.getId()).isEqualTo(subCat1.getId()))
        .anySatisfy((sub) -> assertThat(sub.getId()).isEqualTo(subCat2.getId()))
        .size()
        .isEqualTo(2);

    assertThat(
            entityService.getSubclasses(
                organisation.getId(), repository.getId(), subCat1.getId(), null))
        .isNotEmpty()
        .allSatisfy((sub) -> assertThat(sub.getId()).isEqualTo(subCat3.getId()))
        .size()
        .isEqualTo(1);

    assertThat(
            entityService.getSubclasses(
                organisation.getId(), repository.getId(), subCat2.getId(), null))
        .isNullOrEmpty();
  }

  @Test
  void loadEntity() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(organisation.getId(), new Repository().id("repo"), null);
    Category category =
        (Category)
            new Category()
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.CATEGORY)
                .addTitlesItem(new LocalisableText().text("category").lang("en"));

    assertThat(entityService.createEntity(organisation.getId(), repository.getId(), category))
        .isNotNull();

    assertThat(
            entityService.loadEntity(organisation.getId(), repository.getId(), category.getId(), 1))
        .isNotNull()
        .isInstanceOf(Category.class)
        .satisfies(
            c -> {
              assertThat(c.getId()).isEqualTo(category.getId());
              assertThat(c.getEntityType()).isEqualTo(category.getEntityType());
              assertThat(c.getTitles()).isNotEmpty().size().isEqualTo(1);
            });

    assertThatThrownBy(
            () ->
                entityService.loadEntity(
                    organisation.getId(), repository.getId(), category.getId(), 2))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void updateEntityById() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(organisation.getId(), new Repository().id("repo"), null);
    Category category =
        (Category) new Category().id(UUID.randomUUID().toString()).entityType(EntityType.CATEGORY);

    assertThatCode(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), category))
        .doesNotThrowAnyException();

    Phenotype phenotype =
        (Phenotype)
            new Phenotype()
                .addSuperCategoriesItem(category)
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.SINGLE_PHENOTYPE)
                .addTitlesItem(new LocalisableText().text("Height").lang("en"));

    assertThat(entityService.createEntity(organisation.getId(), repository.getId(), phenotype))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(
            p -> {
              assertThat(p.getVersion()).isEqualTo(1);
              assertThat(p.getTitles()).size().isEqualTo(1);
            });

    phenotype.addTitlesItem(new LocalisableText().text("Größe").lang("de"));

    assertThat(
            entityService.updateEntityById(
                organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(
            p -> {
              assertThat(p.getVersion()).isEqualTo(2);
              assertThat(p.getTitles()).size().isEqualTo(2);
              assertThat(((Phenotype) p).getSuperCategories()).size().isEqualTo(1);
            });

    phenotype.setTitles(List.of(new LocalisableText().text("大きさ").lang("jp")));

    assertThat(
            entityService.updateEntityById(
                organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(
            p -> {
              assertThat(p.getVersion()).isEqualTo(3);
              assertThat(p.getTitles()).size().isEqualTo(1);
              assertThat(((Phenotype) p).getSuperCategories()).size().isEqualTo(1);
            });

    phenotype.setEntityType(EntityType.COMBINED_PHENOTYPE);

    assertThatThrownBy(
            () ->
                entityService.updateEntityById(
                    organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    assertThat(classVersionRepository.findCurrentByClassId(phenotype.getId()))
        .isPresent()
        .hasValueSatisfying(cv -> assertThat(cv.getVersion()).isEqualTo(3));
  }
}
