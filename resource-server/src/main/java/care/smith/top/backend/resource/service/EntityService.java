package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.*;
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
import org.springframework.web.server.ResponseStatusException;

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

    List<UUID> superClasses = new ArrayList<>();
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

    return classToEntity(classRepository.save(cls));
  }

  public Entity loadEntity(String organisationId, String repositoryId, UUID id, Integer version) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    ClassVersion classVersion =
        classVersionRepository
            .findByClassIdAndVersion(cls.getId(), version)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return classVersionToEntity(classVersion);
  }

  @Transactional
  public void deleteEntity(
      String organisationId, String repositoryId, UUID id, Integer version, boolean permanent) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Optional<ClassVersion> optional;
    if (version == null) {
      optional = cls.getCurrentVersion();
    } else {
      optional = classVersionRepository.findByClassIdAndVersion(cls.getId(), version);
    }
    ClassVersion classVersion =
        optional.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (permanent) {
      deleteAnnotations(classVersion);
      expressionRepository.deleteAll(classVersion.getExpressions());
      classVersionRepository.delete(classVersion);
    } else {
      if (!classVersion.isHidden()) classVersionRepository.save(classVersion.hide());
    }
  }

  public Entity updateEntityById(
      String organisationId, String repositoryId, UUID id, Entity entity, List<String> include) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    cls.setCurrentVersion(
        buildClassVersion(entity).setVersion(classRepository.getNextVersion(cls)));

    List<UUID> superClasses = new ArrayList<>();
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

    return classToEntity(classRepository.save(cls));
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
    return classVersionRepository
        .findByRepositoryIdAndNameContainingIgnoreCaseAndTypeAndDataType(
            repositoryId,
            name,
            type.getValue(),
            dataType.getValue(),
            PageRequest.of(page, pageSize))
        .stream()
        .map(this::classVersionToEntity)
        .collect(Collectors.toList());
  }

  /**
   * Get {@link Repository} by repositoryId and directoryId. If the repository does not exist or is
   * not associated with the directory, this method will throw an exception.
   *
   * @param organisationId ID of the {@link Directory}
   * @param repositoryId ID of the {@link Repository}
   * @return
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
      // TODO: convert formula to string or store components as annotations
      //      if (phenotype.getFormula() != null)
      //        classVersion.addAnnotation(new Annotation("formula", phenotype.getFormula(), null));
      if (phenotype.getUnits() != null)
        classVersion.addAnnotations(
            phenotype.getUnits().stream()
                .map(u -> new Annotation("unit", u.getUnit(), null))
                .collect(Collectors.toSet()));
      // TODO: convert expression to string and apply variables
      //      if (phenotype.getExpression() != null)
      //        phenotype.getExpression()
      // TODO: add restrictions
    }

    // TODO: rework class id or apply workaround for codes with missing UUID
    //    if (entity.getCodes() != null) {
    //      entity.getCodes().forEach(c -> {
    //        classRepository.findById(c.getCode()).ifPresent(codeClass ->
    // classVersion.addAnnotation(new Annotation("code", codeClass, null)));
    //      });
    //    }

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

  /**
   * Transforms the given {@link ClassVersion} object to an {@link Entity} object.
   *
   * @param classVersion The {@link ClassVersion} object to be transformed.
   * @return The resulting {@link Entity} object.
   */
  private Entity classVersionToEntity(ClassVersion classVersion) {
    Category entity;

    EntityType entityType =
        EntityType.fromValue(
            classVersion.getAnnotations("type").stream()
                .findFirst()
                .orElseThrow()
                .getStringValue());

    Set<ClassRelation> superClasses = classVersion.getaClass().getSuperClassRelations();

    if (entityType.equals(EntityType.CATEGORY)) {
      entity = new Category();
      if (superClasses != null)
        entity.setSuperCategories(
            superClasses.stream()
                .map(c -> (Category) new Category().id(c.getSuperclass().getId()))
                .collect(Collectors.toList()));
    } else if (entityType.equals(EntityType.PHENOTYPE_GROUP)) {
      entity = new PhenotypeGroup();
    } else {
      entity = new Phenotype();

      // TODO: entity.setDataType();
      // TODO: entity.setExpression();
      // TODO: entity.setSuperPhenotype();
    }

    if (superClasses != null)
      entity.setSuperCategories(
          superClasses.stream()
              .map(c -> (Category) new Category().id(c.getSuperclass().getId()))
              .collect(Collectors.toList()));

    care.smith.top.backend.model.Repository repository =
        new care.smith.top.backend.model.Repository();
    repository.setId(classVersion.getaClass().getRepositoryId());

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

    // TODO: entity.setCodes();
    // TODO: entity.setAuthor(classVersion.getUser()); Map User to UserAccount, or drop UserAccount
    // from top-api model.
    // TODO: entity.setRefer(); <- insert URI

    // if (superClasses != null && superClasses.stream().findFirst().isPresent())
    //   entity.setIndex(superClasses.stream().findFirst().get().getIndex());

    return entity;
  }

  /**
   * Transforms the given {@link Class} object's <u>current version</u> to an {@link Entity} object.
   *
   * @param cls The {@link Class} object to be transformed.
   * @return The resulting {@link Entity} object.
   */
  private Entity classToEntity(Class cls) {
    return classVersionToEntity(
        cls.getCurrentVersion()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Entity had no version!")));
  }
}
