package care.smith.top.backend.service;

import care.smith.top.backend.model.EntityDao;
import care.smith.top.backend.model.EntityVersionDao;
import care.smith.top.backend.model.LocalisableTextDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.repository.*;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.converter.PhenotypeExporter;
import care.smith.top.top_phenotypic_query.converter.PhenotypeImporter;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.SubTypes;

@Service
@Transactional
public class EntityService implements ContentService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private EntityRepository entityRepository;
  @Autowired private EntityVersionRepository entityVersionRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private PhenotypeRepository phenotypeRepository;
  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private UserService userService;

  @Override
  @Cacheable("entityCount")
  public long count() {
    return entityRepository.count();
  }

  @Cacheable("entityCount")
  public long count(EntityType... types) {
    return entityRepository.countByEntityTypeIn(types);
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public int createEntities(
      String organisationId, String repositoryId, List<Entity> entities, List<String> include) {
    Map<String, String> ids = new HashMap<>();

    for (Entity entity :
        entities.stream()
            .sorted(ApiModelMapper::compareByEntityType)
            .collect(Collectors.toList())) {
      createEntity(organisationId, repositoryId, entity, ids, entities);
    }

    return ids.size();
  }

  /**
   * Create an entity, if the entity depends on other entities, depending on the entity type the
   * following will happen:
   *
   * <ul>
   *   <li>category: super categories are created too
   *   <li>abstract phenotype: entities referenced in expressions are created too
   *   <li>restriction: super phenotypes are created too
   * </ul>
   *
   * If you are calling this method multiple times, you should sort the entities with the {@link
   * ApiModelMapper#compareByEntityType(Entity, Entity)} comparator.
   *
   * @param organisationId The organisation ID.
   * @param repositoryId The repository ID.
   * @param data The category to be created.
   * @param ids Hash map containing all IDs of created entities.
   */
  private Entity createEntity(
      String organisationId,
      String repositoryId,
      Entity data,
      Map<String, String> ids,
      List<Entity> entities) {
    if (data == null || ids.containsKey(data.getId())) return data;
    ids.put(data.getId(), null);

    if (ApiModelMapper.isCategory(data)) {
      if (((Category) data).getSuperCategories() != null)
        ((Category) data)
            .setSuperCategories(
                ((Category) data)
                    .getSuperCategories().stream()
                        .map(
                            e ->
                                (Category)
                                    createEntity(
                                        organisationId,
                                        repositoryId,
                                        ApiModelMapper.getEntity(entities, e.getId()),
                                        ids,
                                        entities))
                        .collect(Collectors.toList()));
    } else if (ApiModelMapper.isAbstract(data)) {
      ApiModelMapper.getEntityIdsFromExpression(((Phenotype) data).getExpression())
          .forEach(
              id ->
                  createEntity(
                      organisationId,
                      repositoryId,
                      ApiModelMapper.getEntity(entities, id),
                      ids,
                      entities));
      ((Phenotype) data)
          .setExpression(ApiModelMapper.replaceEntityIds(((Phenotype) data).getExpression(), ids));
    } else if (ApiModelMapper.isRestricted(data)
        && ((Phenotype) data).getSuperPhenotype() != null) {
      String oldId = ((Phenotype) data).getSuperPhenotype().getId();
      createEntity(
          organisationId, repositoryId, ApiModelMapper.getEntity(entities, oldId), ids, entities);
      ((Phenotype) data).getSuperPhenotype().setId(ids.get(oldId));
    }

    Entity entity = null;
    try {
      entity = createEntity(organisationId, repositoryId, data, true);
      ids.put(data.getId(), entity.getId());
    } catch (Exception ignored) {
    }
    return entity;
  }

  private Entity createEntity(
      String organisationId, String repositoryId, Entity data, boolean modifyId) {
    String id = data.getId();
    if (entityRepository.existsById(data.getId())) {
      if (modifyId) id = UUID.randomUUID().toString();
      else throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
    RepositoryDao repository = getRepository(organisationId, repositoryId);

    if (data.getEntityType() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityType is missing");

    if (RepositoryType.CONCEPT_REPOSITORY.equals(repository.getRepositoryType())
        && !EntityType.CATEGORY.equals(data.getEntityType()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "entityType is invalid for concept repository");

    EntityDao entity = new EntityDao(data).id(id).repository(repository);

    if (data instanceof Category && ((Category) data).getSuperCategories() != null)
      for (Category category : ((Category) data).getSuperCategories())
        categoryRepository
            .findByIdAndRepositoryId(category.getId(), repositoryId)
            .ifPresent(entity::addSuperEntitiesItem);

    if (data instanceof Phenotype && ((Phenotype) data).getSuperPhenotype() != null)
      phenotypeRepository
          .findByIdAndRepositoryId(((Phenotype) data).getSuperPhenotype().getId(), repositoryId)
          .ifPresentOrElse(
              entity::addSuperEntitiesItem,
              () -> {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
              });

    entity = entityRepository.save(entity);
    EntityVersionDao entityVersion =
        entityVersionRepository.save(new EntityVersionDao(data).version(1).entity(entity));
    entity.currentVersion(entityVersion);

    return entityRepository.save(entity).toApiModel();
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public Entity createEntity(String organisationId, String repositoryId, Entity data) {
    return createEntity(organisationId, repositoryId, data, false);
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ') "
          + "and hasPermission(#forkingInstruction.organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public List<Entity> createFork(
      String organisationId,
      String repositoryId,
      String id,
      ForkingInstruction forkingInstruction,
      Integer version,
      List<String> include) {
    if (repositoryId.equals(forkingInstruction.getRepositoryId()))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE,
          String.format("Cannot create fork of entity '%s' in the same repository.", id));

    RepositoryDao originRepo = getRepository(organisationId, repositoryId);

    if (!originRepo.getPrimary())
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE,
          String.format(
              "Cannot create fork of entity '%s' from non-primary repository '%s'.",
              id, originRepo.getId()));

    RepositoryDao destinationRepo =
        getRepository(forkingInstruction.getOrganisationId(), forkingInstruction.getRepositoryId());

    Entity entity = loadEntity(organisationId, repositoryId, id, null);

    if (RepositoryType.CONCEPT_REPOSITORY.equals(destinationRepo.getRepositoryType())
        && !EntityType.CATEGORY.equals(entity.getEntityType()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "entityType is invalid for concept repository");

    List<Entity> origins = new ArrayList<>();
    if (ApiModelMapper.isRestricted(entity)) {
      Entity superPhenotype =
          loadEntity(
              organisationId, repositoryId, ((Phenotype) entity).getSuperPhenotype().getId(), null);
      origins.add(superPhenotype);
      origins.add(entity);
    } else {
      origins.add(entity);
      if (forkingInstruction.isCascade())
        origins.addAll(getSubclasses(organisationId, repositoryId, origins.get(0).getId(), null));
    }

    List<Entity> results = new ArrayList<>();
    for (Entity origin : origins) {
      String oldId = origin.getId();
      Optional<EntityDao> fork =
          entityRepository.findByRepositoryIdAndOriginId(destinationRepo.getId(), origin.getId());

      if (!forkingInstruction.isUpdate() && fork.isPresent()) continue;

      if (forkingInstruction.isUpdate() && fork.isPresent()) {
        if (fork.get().getCurrentVersion().getEquivalentEntityVersions().stream()
            .anyMatch(e -> e.getEntity().getId().equals(origin.getId()))) continue;
        origin.setId(fork.get().getId());
        origin.setVersion(fork.get().getCurrentVersion().getVersion() + 1);
        if (origin instanceof Phenotype) {
          ((Phenotype) origin)
              .setSuperCategories(
                  fork.get().getSuperEntities().stream()
                      .map(e -> ((Category) new Category().id(e.getId())))
                      .collect(Collectors.toList()));
        } else if (origin instanceof Category) {
          ((Category) origin)
              .setSuperCategories(
                  fork.get().getSuperEntities().stream()
                      .map(e -> ((Category) new Category().id(e.getId())))
                      .collect(Collectors.toList()));
        }
      }

      if (!forkingInstruction.isUpdate() || fork.isEmpty()) {
        origin.setId(UUID.randomUUID().toString());
        origin.setVersion(1);
        if (origin instanceof Phenotype) ((Phenotype) origin).setSuperCategories(null);
        else if (origin instanceof Category) ((Category) origin).setSuperCategories(null);
      }

      if (origin instanceof Phenotype) {
        Phenotype phenotype = (Phenotype) origin;
        if (phenotype.getSuperPhenotype() != null) {
          Optional<EntityDao> superClass =
              entityRepository.findByRepositoryIdAndOriginId(
                  destinationRepo.getId(), phenotype.getSuperPhenotype().getId());
          if (superClass.isEmpty()) continue;
          phenotype.setSuperPhenotype((Phenotype) new Phenotype().id(superClass.get().getId()));
        }
      }

      if (origin.getVersion() == 1) {
        results.add(
            createEntity(forkingInstruction.getOrganisationId(), destinationRepo.getId(), origin));
        entityRepository.setFork(origin.getId(), oldId);
      } else {
        results.add(
            updateEntityById(
                forkingInstruction.getOrganisationId(),
                destinationRepo.getId(),
                origin.getId(),
                origin,
                null));
      }

      EntityDao createdFork =
          entityRepository
              .findByIdAndRepositoryId(origin.getId(), destinationRepo.getId())
              .orElseThrow();

      EntityDao originDao =
          entityRepository.findByIdAndRepositoryId(oldId, repositoryId).orElseThrow();
      entityRepository.save(createdFork.origin(originDao));
      entityVersionRepository.save(
          createdFork
              .getCurrentVersion()
              .addEquivalentEntityVersionsItem(originDao.getCurrentVersion()));
    }

    return results;
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public void deleteEntity(String organisationId, String repositoryId, String id) {
    getRepository(organisationId, repositoryId);

    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (entity.getSubEntities() != null) {
      if (ApiModelMapper.isAbstract(entity.getEntityType())) {
        entityRepository.deleteAll(entity.getSubEntities());
      } else if (ApiModelMapper.isCategory(entity.getEntityType())) {
        for (EntityDao subEntity : entity.getSubEntities()) {
          subEntity
              .removeSuperEntitiesItem(entity)
              .addAllSuperEntitiesItems(entity.getSuperEntities());
          entityRepository.save(subEntity);
        }
      }
    }

    if (entity.getForks() != null)
      for (EntityDao fork : entity.getForks()) entityRepository.save(fork.origin(null));

    if (entity.getVersions() != null)
      for (EntityVersionDao version : entity.getVersions())
        if (version.getEquivalentEntityVersionOf() != null)
          for (EntityVersionDao eqVersion : version.getEquivalentEntityVersionOf())
            entityVersionRepository.save(eqVersion.removeEquivalentEntityVersionsItem(version));

    entityRepository.delete(entity);
  }

  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public void deleteVersion(
      String organisationId, String repositoryId, String id, Integer version) {
    getRepository(organisationId, repositoryId);
    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    EntityVersionDao entityVersion =
        entityVersionRepository
            .findByEntity_RepositoryIdAndEntityIdAndVersion(repositoryId, id, version)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    EntityVersionDao currentVersion = entity.getCurrentVersion();
    if (currentVersion == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Class does not have a current version.");

    if (entityVersion.equals(currentVersion))
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, "Current version of a class cannot be deleted.");

    EntityVersionDao previous = entityVersion.getPreviousVersion();
    EntityVersionDao next = entityVersion.getNextVersion();
    if (next != null) entityVersionRepository.save(next.previousVersion(previous));

    entityVersionRepository.delete(entityVersion);
  }

  // TODO: restrict access to organisations with read permission and primary repositories
  public List<Entity> getEntities(
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      List<String> repositoryIds,
      Boolean includePrimary,
      Integer page) {
    PageRequest pageRequest = PageRequest.of(page != null ? page - 1 : 0, pageSize);
    return phenotypeRepository
        .findAllByRepositoryIdsAndRepository_PrimaryAndTitleAndEntityTypeAndDataTypeAndItemType(
            repositoryIds, includePrimary, name, type, dataType, itemType, userService.getCurrentUser(), pageRequest)
        .map(EntityDao::toApiModel)
        .toList();
  }

  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public List<Entity> getEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType,
      Integer page) {
    getRepository(organisationId, repositoryId);
    PageRequest pageRequest = PageRequest.of(page != null ? page - 1 : 0, pageSize);

    return phenotypeRepository
        .findAllByRepositoryIdAndTitleAndEntityTypeAndDataTypeAndItemType(
            repositoryId, name, type, dataType, itemType, pageRequest)
        .map(EntityDao::toApiModel)
        .toList();
  }

  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public ForkingStats getForkingStats(
      String organisationId, String repositoryId, String id, List<String> include) {
    getRepository(organisationId, repositoryId);
    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    ForkingStats forkingStats = new ForkingStats();
    EntityDao origin = entity.getOrigin();
    if (origin != null && origin.getCurrentVersion() != null)
      forkingStats.origin(
          new Entity()
              .id(origin.getId())
              .titles(
                  origin.getCurrentVersion().getTitles().stream()
                      .map(LocalisableTextDao::toApiModel)
                      .collect(Collectors.toList())));
    if (entity.getForks() != null)
      entity.getForks().stream()
          .map(
              f -> {
                RepositoryDao repository = f.getRepository();
                return new Entity()
                    .id(f.getId())
                    .repository(new Repository().id(repository.getId()).name(repository.getName()));
              })
          .forEach(forkingStats::addForksItem);
    return forkingStats;
  }

  @Cacheable(
      value = "entities",
      key = "#repositoryId",
      condition = "#name == null && #type == null && #dataType == null")
  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public List<Entity> getRootEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      ItemType itemType) {
    // TODO: filter parameters are ignored
    getRepository(organisationId, repositoryId);
    Pageable pageRequest = Pageable.unpaged();
    return entityRepository
        .findAllByRepositoryIdAndSuperEntitiesEmpty(repositoryId, pageRequest)
        .map(EntityDao::toApiModel)
        .getContent();
  }

  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public List<Entity> getSubclasses(
      String organisationId, String repositoryId, String id, List<String> include) {
    getRepository(organisationId, repositoryId);
    return categoryRepository.findAllByRepositoryIdAndSuperEntities_Id(repositoryId, id).stream()
        .map(EntityDao::toApiModel)
        .collect(Collectors.toList());
  }

  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public List<Entity> getVersions(
      String organisationId, String repositoryId, String id, List<String> include) {
    getRepository(organisationId, repositoryId);
    return entityVersionRepository
        .findAllByEntity_RepositoryIdAndEntityIdOrderByVersionDesc(repositoryId, id)
        .stream()
        .map(EntityVersionDao::toApiModel)
        .collect(Collectors.toList());
  }

  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public Entity loadEntity(String organisationId, String repositoryId, String id, Integer version) {
    getRepository(organisationId, repositoryId);
    if (version == null)
      return entityRepository
          .findByIdAndRepositoryId(id, repositoryId)
          .map(EntityDao::toApiModel)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return entityVersionRepository
        .findByEntity_RepositoryIdAndEntityIdAndVersion(repositoryId, id, version)
        .map(EntityVersionDao::toApiModel)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @CacheEvict(value = "entities", key = "#repositoryId")
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public Entity setCurrentEntityVersion(
      String organisationId,
      String repositoryId,
      String id,
      Integer version,
      List<String> include) {
    getRepository(organisationId, repositoryId);

    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    EntityVersionDao entityVersion =
        entityVersionRepository
            .findByEntity_RepositoryIdAndEntityIdAndVersion(repositoryId, id, version)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return entityRepository.save(entity.currentVersion(entityVersion)).toApiModel();
  }

  @CacheEvict(value = "entities", key = "#repositoryId")
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public Entity updateEntityById(
      String organisationId, String repositoryId, String id, Entity data, List<String> include) {
    getRepository(organisationId, repositoryId);
    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    EntityVersionDao latestVersion = entityVersionRepository.findByEntityIdAndNextVersionNull(id);
    EntityVersionDao newVersion = new EntityVersionDao(data).entity(entity);

    if (latestVersion != null) {
      if (latestVersion.getDataType() != newVersion.getDataType())
        throw new ResponseStatusException(
            HttpStatus.NOT_ACCEPTABLE, "update of data type is forbidden");
      newVersion.previousVersion(latestVersion).version(latestVersion.getVersion() + 1);
    } else {
      newVersion.version(0);
    }

    newVersion = entityVersionRepository.save(newVersion);

    if (!ApiModelMapper.isRestricted(entity.getEntityType())) {
      entity.superEntities(null);
      if (((Category) data).getSuperCategories() != null)
        for (Category category : ((Category) data).getSuperCategories())
          categoryRepository
              .findByIdAndRepositoryId(category.getId(), repositoryId)
              .ifPresent(entity::addSuperEntitiesItem);
    }

    return entityRepository.save(entity.currentVersion(newVersion)).toApiModel();
  }

  @PreAuthorize(
      "hasPermission(#repositoryId, 'care.smith.top.backend.model.RepositoryDao', 'READ')")
  public ByteArrayOutputStream exportRepository(
      String organisationId, String repositoryId, String converter) {
    RepositoryDao repository = getRepository(organisationId, repositoryId);
    Entity[] entities =
        entityRepository
            .findAllByRepositoryId(repositoryId, Pageable.unpaged())
            .map(EntityDao::toApiModel)
            .stream()
            .toArray(Entity[]::new);

    Reflections reflections = new Reflections("care.smith.top");
    Optional<Class<?>> optional =
        reflections.get(SubTypes.of(PhenotypeExporter.class).asClass()).stream()
            .filter(c -> c.getSimpleName().equals(converter))
            .findFirst();

    if (optional.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, String.format("No converter '%s' available.", converter));

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      PhenotypeExporter exporter =
          (PhenotypeExporter) optional.get().getConstructor().newInstance();
      String uri = String.format("http://%s.org/%s", organisationId, repositoryId);
      exporter.write(entities, repository.toApiModel(), uri, stream);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Export failed with error: " + e.getMessage());
    }

    return stream;
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasPermission(#organisationId, 'care.smith.top.backend.model.OrganisationDao', 'WRITE')")
  public void importRepository(
      String organisationId, String repositoryId, String converter, InputStream stream) {
    Reflections reflections = new Reflections("care.smith.top");
    Optional<Class<?>> optional =
        reflections.get(SubTypes.of(PhenotypeImporter.class).asClass()).stream()
            .filter(c -> c.getSimpleName().equals(converter))
            .findFirst();

    if (optional.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, String.format("No converter '%s' available.", converter));

    try {
      PhenotypeImporter importer =
          (PhenotypeImporter) optional.get().getConstructor().newInstance();
      List<Entity> entities = List.of(importer.read(stream));
      createEntities(organisationId, repositoryId, entities, null);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Import failed with error: " + e.getMessage());
    }
  }

  /**
   * Get {@link Repository} by repositoryId and directoryId. If the repository does not exist or is
   * not associated with the directory, this method will throw an exception.
   *
   * @param organisationId ID of the {@link Organisation}
   * @param repositoryId ID of the {@link Repository}
   * @return The matching repository, if it exists.
   */
  private RepositoryDao getRepository(String organisationId, String repositoryId) {
    return repositoryRepository
        .findByIdAndOrganisationId(repositoryId, organisationId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Repository '%s' does not exist!", repositoryId)));
  }
}
