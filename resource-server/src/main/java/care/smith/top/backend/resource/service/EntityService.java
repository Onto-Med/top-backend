package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.*;
import care.smith.top.backend.model.Expression;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.Repository;
import care.smith.top.backend.neo4j_ontology_access.model.*;
import care.smith.top.backend.neo4j_ontology_access.repository.*;
import org.neo4j.cypherdsl.core.*;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class EntityService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private ClassRepository classRepository;
  @Autowired private ClassVersionRepository classVersionRepository;
  @Autowired private AnnotationRepository annotationRepository;
  @Autowired private ExpressionRepository expressionRepository;
  @Autowired private RepositoryRepository repositoryRepository;

  static Statement findEntitiesMatchingCondition(
      String repositoryId, String name, String type, String dataType) {
    Node c = Cypher.node(Class.class.getName()).named("c");
    Node cv = Cypher.node(ClassVersion.class.getName()).named("cv");
    Node a = Cypher.node(Annotation.class.getName()).named("a");
    Relationship cRel = c.relationshipTo(cv, "CURRENT_VERSION");
    Relationship aRel = cv.relationshipTo(a, "HAS_ANNOTATION");

    AtomicReference<StatementBuilder.OngoingReadingWithWhere> statement =
        new AtomicReference<>(
            Cypher.match(cRel)
                .where(c.property("repositoryId").isEqualTo(Cypher.anonParameter(repositoryId)))
                .and(cv.property("hiddenAt").isNull()));

    // TODO: match name/title case-insensitive

    Map<String, String> annotations = new HashMap<>();
    annotations.put("type", type);
    annotations.put("dataType", dataType);

    annotations.forEach(
        (p, v) -> {
          if (p != null) {
            statement.set(
                statement
                    .get()
                    .match(aRel)
                    .where(a.property("property").isEqualTo(Cypher.anonParameter(p)))
                    .and(a.property("stringValue").isEqualTo(Cypher.anonParameter(v))));
          }
        });

    return statement
        .get()
        .returning(
            cv.getRequiredSymbolicName(),
            Functions.collect(cRel),
            Functions.collect(c),
            Functions.collect(aRel),
            Functions.collect(a))
        .build();
  }

  @Transactional
  public Entity createEntity(String organisationId, String repositoryId, Entity entity) {
    if (classRepository.existsById(entity.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    getRepository(organisationId, repositoryId);

    Class cls = new Class(entity.getId());
    cls.setRepositoryId(repositoryId);
    cls.setCurrentVersion(buildClassVersion(entity).setVersion(1));

    List<String> superClasses = new ArrayList<>();
    if (entity instanceof Category) {
      Category category = (Category) entity;
      if (category.getSuperCategories() != null)
        superClasses.addAll(
            category.getSuperCategories().stream().map(Entity::getId).collect(Collectors.toList()));
    }

    if (entity instanceof Phenotype) {
      Phenotype phenotype = (Phenotype) entity;
      if (phenotype.getSuperPhenotype() != null)
        superClasses.add(phenotype.getSuperPhenotype().getId());
    }

    if (!superClasses.isEmpty()) {
      superClasses.forEach(
          c -> {
            Class superClass =
                classRepository
                    .findByIdAndRepositoryId(c, repositoryId)
                    .orElseThrow(
                        () ->
                            new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                String.format("Super class '%s' does not exist!", c)));
            cls.addSuperClassRelation(
                new ClassRelation(superClass, repositoryId, entity.getIndex()));
          });
    }

    return classToEntity(classRepository.save(cls), repositoryId);
  }

  public Entity loadEntity(String organisationId, String repositoryId, String id, Integer version) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Optional<ClassVersion> optional;
    if (version == null) {
      optional = classVersionRepository.findCurrentByClassId(cls.getId());
    } else {
      optional = classVersionRepository.findByClassIdAndVersion(cls.getId(), version);
    }
    ClassVersion classVersion =
        optional.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return classVersionToEntity(classVersion, repositoryId);
  }

  @Transactional
  public void deleteEntity(
      String organisationId, String repositoryId, String id, Integer version, boolean permanent) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Optional<ClassVersion> optional;
    if (version == null) {
      optional = classVersionRepository.findCurrentByClassId(cls.getId());
    } else {
      optional = classVersionRepository.findByClassIdAndVersion(cls.getId(), version);
    }
    ClassVersion classVersion =
        optional.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    classVersionRepository
        .getPreviousUnhidden(classVersion)
        .ifPresent(cv -> classRepository.setCurrent(cls, cv));

    if (permanent) {
      deleteAnnotations(classVersion);
      expressionRepository.deleteAll(classVersion.getExpressions());
      classVersionRepository.delete(classVersion);
    } else {
      if (!classVersion.isHidden()) classVersionRepository.hide(classVersion);
    }
  }

  public Entity updateEntityById(
      String organisationId, String repositoryId, String id, Entity entity, List<String> include) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    ClassVersion newVersion =
        buildClassVersion(entity).setVersion(classRepository.getNextVersion(cls));
    classVersionRepository
        .findCurrentByClassId(cls.getId())
        .ifPresent(
            previousVersion -> {
              previousVersion
                  .getAnnotation("type")
                  .ifPresent(
                      type -> {
                        if (!Objects.equals(
                            type.getStringValue(), entity.getEntityType().getValue()))
                          throw new ResponseStatusException(
                              HttpStatus.CONFLICT, "entityType does not match");
                      });
              newVersion.setPreviousVersion(previousVersion);
            });
    cls.setCurrentVersion(newVersion);

    List<String> superClasses = new ArrayList<>();
    if (entity instanceof Category) {
      Category category = (Category) entity;
      if (category.getSuperCategories() != null)
        superClasses.addAll(
            category.getSuperCategories().stream().map(Entity::getId).collect(Collectors.toList()));
    }

    if (entity instanceof Phenotype) {
      Phenotype phenotype = (Phenotype) entity;
      if (phenotype.getSuperPhenotype() != null)
        superClasses.add(phenotype.getSuperPhenotype().getId());
    }

    if (!superClasses.isEmpty()) {
      superClasses.forEach(
          c ->
              cls.addSuperClassRelation(
                  new ClassRelation(new Class(c), repositoryId, entity.getIndex())));
    }

    return classToEntity(classRepository.save(cls), repositoryId);
  }

  public List<Entity> getEntities(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      EntityType type,
      DataType dataType,
      Integer page) {
    getRepository(organisationId, repositoryId);
    int requestedPage = page != null ? page - 1 : 0;
    return classVersionRepository
        .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
            repositoryId,
            name,
            type != null ? type.getValue() : null,
            dataType != null ? dataType.getValue() : null,
            PageRequest.of(requestedPage, pageSize))
        .stream()
        .map(cv -> classVersionToEntity(cv, repositoryId))
        .collect(Collectors.toList());
  }

  public List<Entity> getRestrictions(String ownerId, Phenotype abstractPhenotype) {
    if (!isAbstract(abstractPhenotype.getEntityType())) return new ArrayList<>();
    return classRepository
        .findSubclasses(abstractPhenotype.getId(), ownerId)
        .map(
            cls -> {
              Optional<Class> classVersion =
                  classRepository.findByIdAndRepositoryId(cls.getId(), ownerId);
              return classVersion.map(aClass -> classToEntity(aClass, ownerId)).orElse(null);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private boolean isAbstract(EntityType entityType) {
    return Arrays.asList(
            EntityType.SINGLE_PHENOTYPE,
            EntityType.COMBINED_PHENOTYPE,
            EntityType.DERIVED_PHENOTYPE)
        .contains(entityType);
  }

  private boolean isRestricted(EntityType entityType) {
    return Arrays.asList(
            EntityType.SINGLE_RESTRICTION,
            EntityType.COMBINED_RESTRICTION,
            EntityType.DERIVED_RESTRICTION)
        .contains(entityType);
  }

  private boolean isPhenotype(EntityType entityType) {
    return isAbstract(entityType) || isRestricted(entityType);
  }

  private boolean isCategory(EntityType entityType) {
    return EntityType.CATEGORY.equals(entityType);
  }

  /**
   * Get {@link Repository} by repositoryId and directoryId. If the repository does not exist or is
   * not associated with the directory, this method will throw an exception.
   *
   * @param organisationId ID of the {@link Directory}
   * @param repositoryId ID of the {@link Repository}
   * @return The matching repository, if it exists.
   */
  private Repository getRepository(String organisationId, String repositoryId) {
    return repositoryRepository
        .findByIdAndSuperDirectoryId(repositoryId, organisationId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Repository '%s' does not exist!", repositoryId)));
  }

  /**
   * Recursively delete all annotations of an annotatable object and its annotations.
   *
   * @param annotatable Annotatable object of which annotations will be deleted.
   */
  private void deleteAnnotations(Annotatable annotatable) {
    annotatable.getAnnotations().forEach(this::deleteAnnotations);
    annotationRepository.deleteAll(annotatable.getAnnotations());
  }

  /**
   * Build a new {@link ClassVersion} object from an {@link Entity} object.
   *
   * @param entity The entity that provides data for the {@link ClassVersion} object fields.
   * @return The resulting {@link ClassVersion} object.
   */
  private ClassVersion buildClassVersion(Entity entity) {
    ClassVersion classVersion = new ClassVersion();

    if (entity.getEquivalentEntities() != null) {
      Set<ClassVersion> equivalentEntities = new HashSet<>();
      entity
          .getEquivalentEntities()
          .forEach(
              e ->
                  classVersionRepository
                      .findByClassIdAndVersion(e.getId(), e.getVersion())
                      .ifPresent(equivalentEntities::add));
      classVersion.addEquivalentClasses(equivalentEntities);
    }

    if (entity instanceof Phenotype) {
      Phenotype phenotype = (Phenotype) entity;
      if (phenotype.getScore() != null)
        classVersion.addAnnotation(
            new Annotation("score", phenotype.getScore().doubleValue(), null));
      if (phenotype.getDataType() != null)
        classVersion.addAnnotation(
            new Annotation("dataType", phenotype.getDataType().getValue(), null));
      if (phenotype.getUnits() != null)
        classVersion.addAnnotations(
            phenotype.getUnits().stream().map(this::fromUnit).collect(Collectors.toSet()));
      if (phenotype.getRestriction() != null)
        classVersion.addAnnotation(fromRestriction(phenotype.getRestriction()));
      if (phenotype.getExpression() != null)
        classVersion.addAnnotation(fromExpression(phenotype.getExpression()));
      if (phenotype.getFormula() != null)
        classVersion.addAnnotation(fromFormula(phenotype.getFormula()));
    }

    if (entity.getCodes() != null) {
      entity
          .getCodes()
          .forEach(
              c ->
                  classRepository
                      .findByIdAndRepositoryId(c.getCode(), c.getCodeSystem().getUri().toString())
                      .ifPresent(
                          codeClass ->
                              classVersion.addAnnotation(new Annotation("code", codeClass, null))));
    }

    if (entity.getEntityType() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityType is missing");

    classVersion.addAnnotation(new Annotation("type", entity.getEntityType().getValue(), null));
    if (entity.getTitles() != null)
      classVersion.addAnnotations(
          entity.getTitles().stream()
              .map(t -> new Annotation("title", t.getText(), t.getLang()))
              .collect(Collectors.toSet()));
    if (entity.getSynonyms() != null)
      classVersion.addAnnotations(
          entity.getSynonyms().stream()
              .map(s -> new Annotation("synonym", s.getText(), s.getLang()))
              .collect(Collectors.toSet()));
    if (entity.getSynonyms() != null)
      classVersion.addAnnotations(
          entity.getDescriptions().stream()
              .map(d -> new Annotation("description", d.getText(), d.getLang()))
              .collect(Collectors.toSet()));

    return classVersion;
  }

  private Annotation fromExpression(Expression expression) {
    // TODO: only allow phenotypes from accessible repositories
    if (expression.getId() != null && ExpressionType.CLASS.equals(expression.getType())) {
      Optional<Class> cls = classRepository.findById(expression.getId());
      if (cls.isPresent()) return new Annotation("expression", cls.get(), null);
    }

    Annotation annotation = new Annotation("expression", expression.getType().getValue(), null);
    if (expression.getOperands() != null)
      annotation.addAnnotations(
          expression.getOperands().stream().map(this::fromExpression).collect(Collectors.toSet()));
    return annotation;
  }

  private Expression toExpression(Annotation annotation) {
    if (annotation.getClassValue() != null)
      return new Expression().id(annotation.getClassValue().getId()).type(ExpressionType.CLASS);

    Expression expression =
        new Expression().type(ExpressionType.fromValue(annotation.getStringValue()));
    if (annotation.getAnnotations() != null)
      expression.setOperands(
          annotation.getAnnotations().stream()
              .map(this::toExpression)
              .collect(Collectors.toList()));
    return expression;
  }

  private Annotation fromFormula(Formula formula) {
    // TODO: only allow phenotypes from accessible repositories
    // TODO: expand formula model in top-api, currently there are not scalars supported
    if (formula.getId() != null
        && FormulaOperator.CLASS.equals(formula.getOperator())
        && classRepository.findById(formula.getId()).isPresent())
      return new Annotation("formula", classRepository.findById(formula.getId()).get(), null);

    Annotation annotation = new Annotation("formula", formula.getOperator().getValue(), null);
    if (formula.getOperands() != null)
      annotation.addAnnotations(
          formula.getOperands().stream().map(this::fromFormula).collect(Collectors.toSet()));
    return annotation;
  }

  private Formula toFormula(Annotation annotation) {
    if (annotation.getClassValue() != null)
      return new Formula().id(annotation.getClassValue().getId()).operator(FormulaOperator.CLASS);

    Formula formula =
        new Formula().operator(FormulaOperator.fromValue(annotation.getStringValue()));
    if (annotation.getAnnotations() != null)
      formula.operands(
          annotation.getAnnotations().stream().map(this::toFormula).collect(Collectors.toList()));
    return formula;
  }

  private Annotation fromRestriction(Restriction restriction) {
    if (restriction == null || restriction.getType() == null) return null;

    Annotation annotation =
        (Annotation)
            new Annotation()
                .setProperty("restriction")
                .addAnnotation(new Annotation("type", restriction.getType().getValue(), null))
                .addAnnotation(new Annotation("negated", restriction.isNegated(), null))
                .addAnnotation(
                    new Annotation("quantor", restriction.getQuantor().getValue(), null));

    if (restriction instanceof NumberRestriction) {
      if (((NumberRestriction) restriction).getMinOperator() != null)
        annotation.addAnnotation(
            new Annotation(
                "minOperator",
                ((NumberRestriction) restriction).getMinOperator().getValue(),
                null));
      if (((NumberRestriction) restriction).getMaxOperator() != null)
        annotation.addAnnotation(
            new Annotation(
                "maxOperator",
                ((NumberRestriction) restriction).getMaxOperator().getValue(),
                null));
      if (((NumberRestriction) restriction).getValues() != null)
        annotation.addAnnotations(
            ((NumberRestriction) restriction)
                .getValues().stream()
                    .map(v -> new Annotation("value", v.doubleValue(), null))
                    .collect(Collectors.toSet()));
    } else if (restriction instanceof StringRestriction) {
      if (((StringRestriction) restriction).getValues() != null)
        annotation.addAnnotations(
            ((StringRestriction) restriction)
                .getValues().stream()
                    .map(v -> new Annotation("value", v, null))
                    .collect(Collectors.toSet()));
    } else if (restriction instanceof DateTimeRestriction) {
      if (((DateTimeRestriction) restriction).getMinOperator() != null)
        annotation.addAnnotation(
            new Annotation(
                "minOperator",
                ((DateTimeRestriction) restriction).getMinOperator().getValue(),
                null));
      if (((DateTimeRestriction) restriction).getMaxOperator() != null)
        annotation.addAnnotation(
            new Annotation(
                "maxOperator",
                ((DateTimeRestriction) restriction).getMaxOperator().getValue(),
                null));
      if (((DateTimeRestriction) restriction).getValues() != null)
        annotation.addAnnotations(
            ((DateTimeRestriction) restriction)
                .getValues().stream()
                    .map(v -> new Annotation("value", v.toInstant(), null))
                    .collect(Collectors.toSet()));
    } else if (restriction instanceof BooleanRestriction) {
      if (((BooleanRestriction) restriction).getValues() != null)
        annotation.addAnnotations(
            ((BooleanRestriction) restriction)
                .getValues().stream()
                    .map(v -> new Annotation("value", v, null))
                    .collect(Collectors.toSet()));
    }

    return annotation;
  }

  /**
   * Transforms the given {@link ClassVersion} object to an {@link Entity} object.
   *
   * @param classVersion The {@link ClassVersion} object to be transformed.
   * @param ownerId The owner this relation belongs to ({@link Repository} or {@link
   *     OntologyVersion}).
   * @return The resulting {@link Entity} object.
   */
  private Entity classVersionToEntity(ClassVersion classVersion, String ownerId) {
    Category entity;

    EntityType entityType =
        EntityType.fromValue(classVersion.getAnnotation("type").orElseThrow().getStringValue());

    Set<ClassVersion> superClasses =
        classVersionRepository.getCurrentSuperClassVersionsByOwnerId(
            classVersion.getaClass(), ownerId);

    if (entityType.equals(EntityType.CATEGORY)) {
      entity = new Category();
    } else if (entityType.equals(EntityType.PHENOTYPE_GROUP)) {
      entity = new PhenotypeGroup();
    } else {
      entity = new Phenotype();

      if (classVersion.getAnnotation("dataType").isPresent())
        ((Phenotype) entity)
            .setDataType(
                DataType.fromValue(classVersion.getAnnotation("dataType").get().getStringValue()));

      if (isRestricted(entityType)) {
        superClasses.stream()
            .findFirst()
            .ifPresent(
                c ->
                    ((Phenotype) entity)
                        .setSuperPhenotype((Phenotype) new Phenotype().id(c.getaClass().getId())));
        classVersion
            .getAnnotation("score")
            .ifPresent(s -> ((Phenotype) entity).setScore(BigDecimal.valueOf(s.getDecimalValue())));
        classVersion
            .getAnnotation("restriction")
            .ifPresent(r -> ((Phenotype) entity).setRestriction(toRestriction(r)));
      } else {
        classVersion
            .getAnnotations("unit")
            .forEach(a -> ((Phenotype) entity).addUnitsItem(toUnit(a)));
      }

      classVersion
          .getAnnotation("expression")
          .ifPresent(a -> ((Phenotype) entity).setExpression(toExpression(a)));
      classVersion
          .getAnnotation("formula")
          .ifPresent(a -> ((Phenotype) entity).setFormula(toFormula(a)));
    }

    if (superClasses != null && !isRestricted(entityType))
      entity.setSuperCategories(
          superClasses.stream()
              .map(c -> (Category) new Category().id(c.getaClass().getId()))
              .collect(Collectors.toList()));

    care.smith.top.backend.model.Repository repository =
        new care.smith.top.backend.model.Repository()
            .id(classVersion.getaClass().getRepositoryId());

    entity.setRepository(repository);
    entity.setId(classVersion.getaClass().getId());
    entity.setVersion(classVersion.getVersion());
    entity.setEntityType(entityType);
    entity.setCreatedAt(classVersion.getCreatedAtOffset());
    entity.setHiddenAt(classVersion.getHiddenAtOffset());

    if (classVersion.getEquivalentClasses() != null)
      classVersion
          .getEquivalentClasses()
          .forEach(
              e -> {
                Entity equivalentEntity = new Entity();
                equivalentEntity.setVersion(e.getVersion());
                equivalentEntity.setId(e.getaClass().getId());
                entity.addEquivalentEntitiesItem(equivalentEntity);
              });

    PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(entity);
    Arrays.asList("title", "synonym", "description")
        .forEach(
            p ->
                accessor.setPropertyValue(
                    p + "s",
                    annotationRepository.findByClassVersionAndProperty(classVersion, p).stream()
                        .map(
                            a ->
                                new LocalisableText()
                                    .text(a.getStringValue())
                                    .lang(a.getLanguage()))
                        .collect(Collectors.toList())));

    entity.setCodes(
        annotationRepository.findByClassVersionAndProperty(classVersion, "code").stream()
            .map(
                a ->
                    new Code()
                        .code(a.getClassValue().getId())
                        .codeSystem(
                            new CodeSystem().uri(URI.create(a.getClassValue().getRepositoryId()))))
            .collect(Collectors.toList()));

    // TODO: entity.setAuthor(classVersion.getUser()); Map User to UserAccount, or drop UserAccount
    // from top-api model.
    // TODO: entity.setRefer(); <- insert URI

    if (classVersion.getaClass().getSuperClassRelations() != null)
      classVersion.getaClass().getSuperClassRelations().stream()
          .findFirst()
          .ifPresent(sc -> entity.setIndex(sc.getIndex()));

    return entity;
  }

  private Restriction toRestriction(Annotation annotation) {
    if (annotation == null) return null;
    if (annotation.getAnnotation("type").isEmpty()) return null;
    DataType type = DataType.fromValue(annotation.getAnnotation("type").get().getStringValue());

    Restriction restriction;

    if (type == DataType.STRING) {
      restriction = new StringRestriction();
      annotation
          .getAnnotations("value")
          .forEach(v -> ((StringRestriction) restriction).addValuesItem(v.getStringValue()));
    } else if (type == DataType.NUMBER) {
      restriction = new NumberRestriction();
      annotation
          .getAnnotations("value")
          .forEach(
              v ->
                  ((NumberRestriction) restriction)
                      .addValuesItem(BigDecimal.valueOf(v.getDecimalValue())));
      annotation.getAnnotations("minOperator").stream()
          .findFirst()
          .ifPresent(
              o ->
                  ((NumberRestriction) restriction)
                      .setMinOperator(RestrictionOperator.fromValue(o.getStringValue())));
      annotation.getAnnotations("maxOperator").stream()
          .findFirst()
          .ifPresent(
              o ->
                  ((NumberRestriction) restriction)
                      .setMaxOperator(RestrictionOperator.fromValue(o.getStringValue())));
    } else if (type == DataType.DATE_TIME) {
      restriction = new DateTimeRestriction();
      annotation
          .getAnnotations("value")
          .forEach(
              v ->
                  ((DateTimeRestriction) restriction)
                      .addValuesItem(v.getDateValue().atOffset(ZoneOffset.UTC)));
      annotation.getAnnotations("minOperator").stream()
          .findFirst()
          .ifPresent(
              o ->
                  ((DateTimeRestriction) restriction)
                      .setMinOperator(RestrictionOperator.fromValue(o.getStringValue())));
      annotation.getAnnotations("maxOperator").stream()
          .findFirst()
          .ifPresent(
              o ->
                  ((DateTimeRestriction) restriction)
                      .setMaxOperator(RestrictionOperator.fromValue(o.getStringValue())));
    } else if (type == DataType.BOOLEAN) {
      restriction = new BooleanRestriction();
      annotation
          .getAnnotations("value")
          .forEach(v -> ((BooleanRestriction) restriction).addValuesItem(v.getBooleanValue()));
    } else return null;

    restriction.setType(type);
    annotation.getAnnotation("negated").ifPresent(a -> restriction.setNegated(a.getBooleanValue()));
    annotation
        .getAnnotation("quantor")
        .ifPresent(a -> restriction.setQuantor(Quantor.fromValue(a.getStringValue())));

    return restriction;
  }

  private Unit toUnit(Annotation annotation) {
    if (annotation == null
        || !"unit".equals(annotation.getProperty())
        || !StringUtils.hasText(annotation.getStringValue())) return null;

    Unit unit = new Unit().unit(annotation.getStringValue());
    annotation.getAnnotation("preferred").ifPresent(a -> unit.preferred(a.getBooleanValue()));

    return unit;
  }

  private Annotation fromUnit(Unit unit) {
    if (unit == null || !StringUtils.hasText(unit.getUnit())) return null;

    return (Annotation)
        new Annotation()
            .setProperty("unit")
            .setStringValue(unit.getUnit())
            .addAnnotation(
                new Annotation().setProperty("preferred").setBooleanValue(unit.isPreferred()));
  }

  /**
   * Transforms the given {@link Class} object's <u>current version</u> to an {@link Entity} object.
   *
   * @param cls The {@link Class} object to be transformed.
   * @return The resulting {@link Entity} object.
   */
  private Entity classToEntity(Class cls, String ownerId) {
    return classVersionToEntity(
        cls.getCurrentVersion()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Entity had no version!")),
        ownerId);
  }
}
