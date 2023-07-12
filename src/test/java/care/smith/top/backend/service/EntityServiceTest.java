package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.*;

import care.smith.top.backend.model.EntityDao;
import care.smith.top.backend.util.TopJsonFormat;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.c2reasoner.functions.bool.Not;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EntityServiceTest extends AbstractTest {
  @Test
  void exportRepository() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository1 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository()
                .id("repo1")
                .organisation(organisation)
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Repository repository2 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository()
                .id("repo2")
                .organisation(organisation)
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Phenotype phenotype =
        (Phenotype)
            new Phenotype()
                .dataType(DataType.STRING)
                .id("single_phen")
                .entityType(EntityType.SINGLE_PHENOTYPE);
    entityService.createEntity(organisation.getId(), repository1.getId(), phenotype);

    ByteArrayInputStream export =
        new ByteArrayInputStream(
            entityService
                .exportRepository(
                    organisation.getId(), repository1.getId(), TopJsonFormat.class.getSimpleName())
                .toByteArray());
    entityService.importRepository(
        organisation.getId(), repository2.getId(), TopJsonFormat.class.getSimpleName(), export);

    assertThat(
            entityService.getEntitiesByRepositoryId(
                organisation.getId(), repository2.getId(), null, null, null, null, null, 1))
        .allSatisfy(
            e -> {
              assertThat(e.getId()).isNotEqualTo(phenotype.getId());
              assertThat(e.getEntityType()).isEqualTo(phenotype.getEntityType());
              assertThat(((Phenotype) e).getDataType()).isEqualTo(phenotype.getDataType());
            })
        .size()
        .isEqualTo(1);
  }

  @Test
  void importRepository() {}

  @Test
  void createEntities() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository()
                .id("repo")
                .organisation(organisation)
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    entityService.createEntity(
        organisation.getId(),
        repository.getId(),
        new Phenotype()
            .dataType(DataType.STRING)
            .id("single_phen")
            .entityType(EntityType.SINGLE_PHENOTYPE));

    Category invalidCategory = (Category) new Category().id("invalid_cat");
    Category superCategory =
        (Category)
            new Category()
                .addSuperCategoriesItem(invalidCategory)
                .id("super_cat")
                .entityType(EntityType.CATEGORY);
    Category subCategory =
        (Category)
            new Category()
                .addSubCategoriesItem(superCategory)
                .id("sub_cat")
                .entityType(EntityType.CATEGORY);
    Phenotype singlePhenotype =
        (Phenotype)
            new Phenotype()
                .dataType(DataType.NUMBER)
                .entityType(EntityType.SINGLE_PHENOTYPE)
                .id("single_phen");
    Phenotype compositePhenotype =
        (Phenotype)
            new Phenotype()
                .expression(new Expression().entityId(singlePhenotype.getId()))
                .entityType(EntityType.COMPOSITE_PHENOTYPE)
                .id("composite_phen");
    Phenotype restriction =
        (Phenotype)
            new Phenotype()
                .restriction(
                    new NumberRestriction()
                        .minOperator(RestrictionOperator.GREATER_THAN)
                        .addValuesItem(BigDecimal.valueOf(15))
                        .type(DataType.NUMBER))
                .dataType(DataType.BOOLEAN)
                .entityType(EntityType.COMPOSITE_RESTRICTION)
                .id("res");

    SingleConcept singleConcept =
        (SingleConcept)
            new SingleConcept()
                .titles(List.of(new LocalisableText().text("Single Concept 1").lang("en")))
                .entityType(EntityType.SINGLE_CONCEPT)
                .id("sing_con");
    SingleConcept subConcept =
        (SingleConcept)
            new SingleConcept()
                .superConcepts(List.of(singleConcept))
                .entityType(EntityType.SINGLE_CONCEPT)
                .id("sub_con");
    CompositeConcept compositeConcept =
        (CompositeConcept)
            new CompositeConcept()
                .expression(care.smith.top.top_document_query.functions.Not.of(singleConcept))
                .entityType(EntityType.COMPOSITE_CONCEPT)
                .id("comp_con");

    List<Entity> bulk =
        Arrays.asList(
            compositePhenotype,
            singlePhenotype,
            superCategory,
            restriction,
            subCategory,
            singlePhenotype,
            subCategory,
            invalidCategory,
            singleConcept,
            subConcept,
            compositeConcept);

    assertThatCode(
            () ->
                entityService.createEntities(organisation.getId(), repository.getId(), bulk, null))
        .doesNotThrowAnyException();
    assertThat(entityService.count()).isEqualTo(9);
    assertThatThrownBy(
            () ->
                entityService.loadEntity(
                    organisation.getId(), repository.getId(), invalidCategory.getId(), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    assertThat(
            entityService.loadEntity(
                organisation.getId(), repository.getId(), compositePhenotype.getId(), null))
        .satisfies(
            e ->
                assertThat(((Phenotype) e).getExpression())
                    .isNotNull()
                    .satisfies(
                        ex ->
                            assertThat(ex.getEntityId())
                                .isNotNull()
                                .isNotEqualTo(singlePhenotype.getId())));
  }

  @Test
  void createFork() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository1 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository()
                .id("repo1")
                .primary(true)
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Repository repository2 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo2").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Repository repository3 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo3").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);

    Entity origin =
        entityService.createEntity(
            organisation.getId(),
            repository1.getId(),
            new Entity()
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.CATEGORY)
                .addTitlesItem(new LocalisableText().lang("en").text("title")));

    ForkingInstruction forkingInstruction =
        new ForkingInstruction()
            .organisationId(organisation.getId())
            .repositoryId(repository1.getId());

    assertThatThrownBy(
            () ->
                entityService.createFork(
                    organisation.getId(),
                    repository1.getId(),
                    origin.getId(),
                    forkingInstruction,
                    null,
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_ACCEPTABLE);

    assertThatCode(
            () ->
                entityService.createFork(
                    organisation.getId(),
                    repository1.getId(),
                    origin.getId(),
                    forkingInstruction.repositoryId(repository2.getId()),
                    null,
                    null))
        .doesNotThrowAnyException();

    assertThatCode(
            () ->
                entityService.createFork(
                    organisation.getId(),
                    repository1.getId(),
                    origin.getId(),
                    forkingInstruction.repositoryId(repository3.getId()),
                    null,
                    null))
        .doesNotThrowAnyException();

    assertThat(
            entityService.getForkingStats(
                organisation.getId(), repository1.getId(), origin.getId(), null))
        .isNotNull()
        .satisfies(
            s ->
                assertThat(s.getForks())
                    .isNotEmpty()
                    .anyMatch(f -> repository2.getId().equals(f.getRepository().getId()))
                    .anyMatch(f -> repository3.getId().equals(f.getRepository().getId()))
                    .size()
                    .isEqualTo(2));

    Optional<EntityDao> fork1 =
        entityRepository.findAllByRepositoryId(repository2.getId(), Pageable.unpaged()).stream()
            .findFirst();
    assertThat(fork1).isPresent();

    origin.addTitlesItem(new LocalisableText().lang("de").text("Titel"));
    assertThatCode(
            () ->
                entityService.updateEntityById(
                    organisation.getId(), repository1.getId(), origin.getId(), origin, null))
        .doesNotThrowAnyException();

    assertThat(
            entityService.createFork(
                organisation.getId(),
                repository1.getId(),
                origin.getId(),
                forkingInstruction.repositoryId(repository3.getId()).update(true),
                null,
                null))
        .size()
        .isEqualTo(1);

    assertThat(
            entityRepository.findByRepositoryIdAndOriginId(
                forkingInstruction.getRepositoryId(), origin.getId()))
        .isPresent()
        .satisfies(f -> assertThat(f.get().getCurrentVersion().getVersion()).isEqualTo(2));

    assertThat(entityVersionRepository.findAll()).isNotEmpty().size().isEqualTo(5);

    assertThatCode(
            () ->
                entityService.deleteEntity(
                    organisation.getId(), repository2.getId(), fork1.get().getId(), null))
        .doesNotThrowAnyException();

    assertThat(
            entityService.getForkingStats(
                organisation.getId(), repository1.getId(), origin.getId(), null))
        .isNotNull()
        .satisfies(s -> assertThat(s.getForks()).size().isEqualTo(1));

    assertThatCode(
            () ->
                entityService.deleteEntity(
                    organisation.getId(), repository1.getId(), origin.getId(), null))
        .doesNotThrowAnyException();
    assertThat(entityRepository.existsById(origin.getId())).isEqualTo(false);
    assertThat(entityRepository.findAll())
        .isNotEmpty()
        .allMatch(e -> e.getOrigin() == null)
        .size()
        .isEqualTo(1);
    assertThat(entityVersionRepository.findAll()).isNotEmpty().size().isEqualTo(2);
  }

  @Test
  void createEntity() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    CodeSystem codeSystem = new CodeSystem().uri(URI.create("http://loinc.org"));

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
                .code("1234")
                .uri(URI.create("http://loing.org/1234"))
                .codeSystem(codeSystem));

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
              assertThat(c.getDescriptions()).size().isEqualTo(1);
              assertThat(c.getEquivalentEntities()).isNullOrEmpty();
              assertThat(c.getCodes()).size().isEqualTo(1);
              assertThat(c.getCodes().get(0).getCode())
                  .isEqualTo(category.getCodes().get(0).getCode());
              assertThat(c.getCodes().get(0).getUri())
                  .isEqualTo(category.getCodes().get(0).getUri());
              assertThat(c.getCreatedAt()).isNotNull();
              assertThat(c.getSynonyms()).size().isEqualTo(1);
              assertThat(c.getSynonyms().get(0)).isEqualTo(category.getSynonyms().get(0));
              assertThat(c.getVersion()).isEqualTo(1);
              assertThat(c.getRepository())
                  .isNotNull()
                  .hasFieldOrPropertyWithValue("id", repository.getId());
              assertThat(((Category) c).getSuperCategories()).isNullOrEmpty();
              assertThat(((Category) c).getSubCategories()).isNotNull().isEmpty();
              assertThat(((Category) c).getPhenotypes()).isNotNull().isEmpty();
            });

    assertThatThrownBy(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), category))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

    /* Create abstract phenotype */
    Phenotype abstractPhenotype =
        (Phenotype)
            new Phenotype()
                .unit("cm")
                .expression(
                    new Expression()
                        .functionId(Not.get().getFunction().getId())
                        .addArgumentsItem(new Expression().functionId("entity")))
                .addSuperCategoriesItem(category)
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
              assertThat(((Phenotype) p).getUnit()).isEqualTo("cm");
              assertThat(((Phenotype) p).getExpression())
                  .isNotNull()
                  .satisfies(
                      e -> {
                        assertThat(e.getFunctionId())
                            .isEqualTo(abstractPhenotype.getExpression().getFunctionId());
                        assertThat(e.getArguments()).size().isEqualTo(1);
                      });
              assertThat(((Phenotype) p).getPhenotypes()).isNotNull().isEmpty();
            });

    /* Create restricted phenotype */
    Phenotype restrictedPhenotype1 =
        (Phenotype)
            new Phenotype()
                .restriction(
                    new NumberRestriction()
                        .addValuesItem(BigDecimal.valueOf(50))
                        .minOperator(RestrictionOperator.GREATER_THAN)
                        .quantifier(Quantifier.MIN)
                        .cardinality(1)
                        .type(DataType.NUMBER))
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
              assertThat(((Phenotype) rp).getRestriction())
                  .isNotNull()
                  .isInstanceOf(NumberRestriction.class)
                  .satisfies(
                      r -> {
                        assertThat(r.getQuantifier()).isEqualTo(Quantifier.MIN);
                        assertThat(r.getCardinality()).isEqualTo(1);
                        assertThat(((NumberRestriction) r).getMaxOperator()).isNull();
                        assertThat(((NumberRestriction) r).getMinOperator())
                            .isNotNull()
                            .isEqualTo(RestrictionOperator.GREATER_THAN);
                        assertThat(((NumberRestriction) r).getValues()).size().isEqualTo(2);
                        assertThat(((NumberRestriction) r).getValues().get(0))
                            .isEqualTo(BigDecimal.valueOf(50));
                        assertThat(((NumberRestriction) r).getValues().get(1)).isNull();
                      });
            });

    Phenotype restrictedPhenotype2 =
        new Phenotype()
            .restriction(
                new NumberRestriction()
                    .addValuesItem(null)
                    .addValuesItem(BigDecimal.valueOf(50))
                    .maxOperator(RestrictionOperator.LESS_THAN_OR_EQUAL_TO)
                    .quantifier(Quantifier.ALL)
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
              assertThat(((Phenotype) rp).getDataType()).isEqualTo(abstractPhenotype.getDataType());
              assertThat(((Phenotype) rp).getRestriction())
                  .isNotNull()
                  .isInstanceOf(NumberRestriction.class)
                  .satisfies(
                      r -> {
                        assertThat(r.getQuantifier()).isEqualTo(Quantifier.ALL);
                        assertThat(((NumberRestriction) r).getMinOperator()).isNull();
                        assertThat(((NumberRestriction) r).getMaxOperator())
                            .isNotNull()
                            .isEqualTo(RestrictionOperator.LESS_THAN_OR_EQUAL_TO);
                        assertThat(((NumberRestriction) r).getValues()).size().isEqualTo(2);
                        assertThat(((NumberRestriction) r).getValues().get(0)).isNull();
                        assertThat(((NumberRestriction) r).getValues().get(1))
                            .isEqualTo(BigDecimal.valueOf(50));
                      });
            });

    assertThat(phenotypeRepository.findById(abstractPhenotype.getId()))
        .isPresent()
        .hasValueSatisfying(
            e -> assertThat(e.getRepository().getId()).isEqualTo(repository.getId()));

    assertThat(
            entityService.getSubclasses(
                organisation.getId(), repository.getId(), abstractPhenotype.getId(), null))
        .isNotEmpty()
        .size()
        .isEqualTo(2);

    /* Create single concept */
    SingleConcept singleConcept =
        (SingleConcept)
            new SingleConcept()
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.SINGLE_CONCEPT);

    assertThat(entityService.createEntity(organisation.getId(), repository.getId(), singleConcept))
        .isNotNull()
        .isInstanceOf(Concept.class)
        .satisfies(
            sc -> {
              assertThat(sc.getId()).isEqualTo(singleConcept.getId());
              assertThat(sc.getVersion()).isEqualTo(1);
              assertThat(sc.getEntityType()).isEqualTo(singleConcept.getEntityType());
              assertThat(((Concept) sc).getSuperConcepts()).isNull();
            });
    /* Create single concept */
    Concept compositeConcept =
        (CompositeConcept)
            new CompositeConcept()
                .expression(
                    care.smith.top.top_document_query.functions.Not.of(singleConcept.getId()))
                .superConcepts(List.of(singleConcept))
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.COMPOSITE_CONCEPT);

    assertThat(
            entityService.createEntity(organisation.getId(), repository.getId(), compositeConcept))
        .isNotNull()
        .isInstanceOf(Concept.class)
        .satisfies(
            sc -> {
              assertThat(sc.getId()).isEqualTo(compositeConcept.getId());
              assertThat(sc.getVersion()).isEqualTo(1);
              assertThat(sc.getEntityType()).isEqualTo(compositeConcept.getEntityType());
              assertThat(((Concept) sc).getSuperConcepts()).isNotNull();
              assertThat(((CompositeConcept) sc).getExpression()).isNotNull();
            });

    assertThat(categoryRepository.count()).isEqualTo(1);
    assertThat(entityRepository.count()).isEqualTo(6);
    assertThat(conceptRepository.count()).isEqualTo(2);
  }

  @Test
  void deleteVersion() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
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

    assertThat(entityRepository.findById(phenotype.getId()))
        .isPresent()
        .hasValueSatisfying(e -> assertThat(e.getCurrentVersion().getVersion()).isEqualTo(3));

    // Delete
    assertThatCode(
            () ->
                entityService.deleteVersion(
                    organisation.getId(), repository.getId(), phenotype.getId(), 2))
        .doesNotThrowAnyException();

    assertThat(entityRepository.findById(phenotype.getId()))
        .isPresent()
        .hasValueSatisfying(
            e -> {
              assertThat(e.getCurrentVersion().getVersion()).isEqualTo(3);
              assertThat(e.getCurrentVersion().getPreviousVersion())
                  .isNotNull()
                  .satisfies(prev -> assertThat(prev.getVersion()).isEqualTo(1));
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
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
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
                entityService.deleteEntity(
                    organisation.getId(), repository.getId(), "invalid id", null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

    assertThatCode(
            () ->
                entityService.deleteEntity(
                    organisation.getId(), repository.getId(), phenotype.getId(), null))
        .doesNotThrowAnyException();

    assertThatThrownBy(
            () ->
                entityService.loadEntity(
                    organisation.getId(), repository.getId(), phenotype.getId(), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
  }

  @Test
  void getEntities() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository1 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository()
                .id("repo1")
                .primary(true)
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Repository repository2 =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository()
                .id("repo2")
                .primary(false)
                .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Phenotype entity1 =
        (Phenotype)
            new Phenotype()
                .dataType(DataType.NUMBER)
                .itemType(ItemType.OBSERVATION)
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.SINGLE_PHENOTYPE)
                .titles(
                    Collections.singletonList(
                        new LocalisableText().lang("en").text("example test")));

    Category entity2 =
        (Category)
            new Category()
                .id(UUID.randomUUID().toString())
                .entityType(EntityType.CATEGORY)
                .titles(
                    Collections.singletonList(new LocalisableText().lang("en").text("example")));

    assertThat(entityService.createEntity(organisation.getId(), repository1.getId(), entity1))
        .isNotNull()
        .isInstanceOf(Phenotype.class)
        .satisfies(p -> assertThat(p.getTitles()).isNotEmpty().size().isEqualTo(1));

    assertThat(entityService.createEntity(organisation.getId(), repository2.getId(), entity2))
        .isNotNull()
        .isInstanceOf(Category.class)
        .satisfies(p -> assertThat(p.getTitles()).isNotEmpty().size().isEqualTo(1));

    assertThat(
            entityService.getEntities(
                null,
                null,
                Collections.singletonList(EntityType.SINGLE_PHENOTYPE),
                null,
                null,
                null,
                null,
                null))
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .size()
        .isEqualTo(1);

    assertThat(entityService.getEntities(null, null, null, null, null, null, null, null))
        .isNotEmpty()
        .anySatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .anySatisfy(e -> assertThat(e.getId()).isEqualTo(entity2.getId()))
        .size()
        .isEqualTo(2);

    assertThat(entityService.getEntities(null, null, null, null, null, null, false, null))
        .isNotEmpty()
        .size()
        .isEqualTo(2);

    assertThat(entityService.getEntities(null, null, null, null, null, null, true, null))
        .isNotEmpty()
        .size()
        .isEqualTo(2);

    assertThat(
            entityService.getEntities(
                null,
                null,
                null,
                null,
                null,
                Collections.singletonList(repository1.getId()),
                false,
                null))
        .isNotEmpty()
        .size()
        .isEqualTo(1);

    assertThat(entityService.getEntities(null, "xample", null, null, null, null, null, null))
        .isNotEmpty()
        .anySatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .anySatisfy(e -> assertThat(e.getId()).isEqualTo(entity2.getId()))
        .size()
        .isEqualTo(2);

    assertThat(entityService.getEntities(null, "test", null, null, null, null, null, null))
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .size()
        .isEqualTo(1);

    assertThat(entityService.getEntities(null, null, null, DataType.NUMBER, null, null, null, null))
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .size()
        .isEqualTo(1);

    assertThat(
            entityService.getEntities(null, null, null, DataType.BOOLEAN, null, null, null, null))
        .isNullOrEmpty();

    assertThat(
            entityService.getEntities(
                null, null, null, null, ItemType.OBSERVATION, null, null, null))
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .size()
        .isEqualTo(1);

    assertThat(
            entityService.getEntities(
                null, null, null, null, ItemType.ALLERGY_INTOLERANCE, null, null, null))
        .isNullOrEmpty();

    assertThat(
            entityService.getEntitiesByRepositoryId(
                organisation.getId(), repository1.getId(), null, "xample", null, null, null, null))
        .isNotEmpty()
        .allSatisfy(e -> assertThat(e.getId()).isEqualTo(entity1.getId()))
        .size()
        .isEqualTo(1);
  }

  @Test
  void getSubclasses() {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);

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
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
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
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);
    Category category =
        (Category) new Category().id(UUID.randomUUID().toString()).entityType(EntityType.CATEGORY);

    assertThatCode(
            () -> entityService.createEntity(organisation.getId(), repository.getId(), category))
        .doesNotThrowAnyException();

    Phenotype phenotype =
        (Phenotype)
            new Phenotype()
                .dataType(DataType.NUMBER)
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

    phenotype.setEntityType(EntityType.COMPOSITE_PHENOTYPE);
    assertThat(
            entityService.updateEntityById(
                organisation.getId(), repository.getId(), phenotype.getId(), phenotype, null))
        .isNotNull()
        .satisfies(p -> assertThat(p.getEntityType()).isEqualTo(EntityType.SINGLE_PHENOTYPE));

    assertThat(entityRepository.findById(phenotype.getId()))
        .isPresent()
        .hasValueSatisfying(e -> assertThat(e.getCurrentVersion().getVersion()).isEqualTo(4));

    assertThatThrownBy(
            () ->
                entityService.updateEntityById(
                    organisation.getId(),
                    repository.getId(),
                    phenotype.getId(),
                    phenotype.dataType(DataType.STRING),
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_ACCEPTABLE);
  }
}
