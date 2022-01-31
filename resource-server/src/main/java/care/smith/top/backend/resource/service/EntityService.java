package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Category;
import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.LocalisableText;
import care.smith.top.backend.model.Phenotype;
import care.smith.top.backend.neo4j_ontology_access.model.Class;
import care.smith.top.backend.neo4j_ontology_access.model.*;
import care.smith.top.backend.neo4j_ontology_access.repository.*;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntityService {
  @Autowired ClassRepository classRepository;
  @Autowired ClassVersionRepository classVersionRepository;
  @Autowired AnnotationRepository annotationRepository;
  @Autowired ExpressionRepository expressionRepository;
  @Autowired RepositoryRepository repositoryRepository;

  @Transactional
  public Entity createEntity(String organisationId, String repositoryId, Entity entity) {
    if (classRepository.existsById(entity.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    getRepository(organisationId, repositoryId);

    Class cls = new Class(entity.getId());
    cls.setRepositoryId(repositoryId);
    cls.createVersion(buildClassVersion(entity), true);

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

  public Entity loadEntity(
      String organisationId, String repositoryId, UUID id, Integer version) {
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
      optional = cls.getVersions().stream().filter(v -> v.getVersion() == version).findFirst();
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
      String organisationId,
      String repositoryId,
      UUID id,
      Entity entity,
      List<String> include) {
    Repository repository = getRepository(organisationId, repositoryId);
    Class cls =
        classRepository
            .findByIdAndRepositoryId(id, repository.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    cls.createVersion(buildClassVersion(entity), true);
    return classToEntity(classRepository.save(cls));
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
    // TODO: add all annotations, expressions and properties
    return (ClassVersion)
        new ClassVersion()
            .addAnnotations(
                entity.getTitles().stream()
                    .map(t -> new Annotation("title", t.getText(), t.getLang(), null))
                    .collect(Collectors.toSet()))
            .addAnnotations(
                entity.getSynonyms().stream()
                    .map(s -> new Annotation("synonym", s.getText(), s.getLang(), null))
                    .collect(Collectors.toSet()))
            .addAnnotations(
                entity.getDescriptions().stream()
                    .map(d -> new Annotation("description", d.getText(), d.getLang(), null))
                    .collect(Collectors.toSet()));
  }

  /**
   * Transforms the given {@link ClassVersion} object to an {@link Entity} object.
   *
   * @param classVersion The {@link ClassVersion} object to be transformed.
   * @return The resulting {@link Entity} object.
   */
  private Entity classVersionToEntity(ClassVersion classVersion) {
    Entity                                  entity     = new Entity();
    care.smith.top.backend.model.Repository repository = new care.smith.top.backend.model.Repository();
    repository.setId(classVersion.getaClass().getRepositoryId());

    entity.setRepository(repository);
    entity.setId(classVersion.getaClass().getId());
    entity.setVersion(classVersion.getVersion());

    Set<ClassRelation> superClasses = classVersion.getaClass().getSuperClassRelations();
    if (superClasses != null && superClasses.stream().findFirst().isPresent())
      entity.setIndex(superClasses.stream().findFirst().get().getIndex());
    entity.setCreatedAt(classVersion.getCreatedAtOffset());
    entity.setHiddenAt(classVersion.getHiddenAtOffset());
    // TODO: entity.setAuthor(classVersion.getUser()); Map User to UserAccount, or drop UserAccount
    // from top-api model.
    // TODO: entity.setRefer(); <- insert URI

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

    // TODO: entity.setEntityType();
    // TODO: entity.setCodes();

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
