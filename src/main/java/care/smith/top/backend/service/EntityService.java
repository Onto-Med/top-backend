package care.smith.top.backend.service;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.repository.jpa.*;
import care.smith.top.backend.repository.ols.CodeRepository;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.converter.PhenotypeExporter;
import care.smith.top.top_phenotypic_query.converter.PhenotypeImporter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EntityService implements ContentService {
  private static final String PLUGIN_PACKAGE = "care.smith.top";

  private final Logger LOGGER = Logger.getLogger(EntityService.class.getName());

  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Value("${spring.max-batch-size:100}")
  private int maxBatchSize;

  @Autowired private EntityRepository entityRepository;
  @Autowired private EntityVersionRepository entityVersionRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private PhenotypeRepository phenotypeRepository;
  @Autowired private ConceptRepository conceptRepository;
  @Autowired private RepositoryRepository repositoryRepository;
  @Autowired private UserService userService;
  @Autowired private CodeRepository codeRepository;

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
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public int createEntities(
      String organisationId, String repositoryId, List<Entity> entities, List<String> include) {
    Map<String, String> ids = new HashMap<>();

    if (entities.size() > maxBatchSize)
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE,
          String.format("Batch size %d exceeds maximum %d.", entities.size(), maxBatchSize));

    for (Entity entity :
        entities.stream().sorted(ApiModelMapper::compare).collect(Collectors.toList())) {
      createEntity(organisationId, repositoryId, entity, ids, entities);
    }

    return ids.size();
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public Entity createEntity(String organisationId, String repositoryId, Entity data) {
    return createEntity(organisationId, repositoryId, data, false);
  }

  @Caching(
      evict = {
        @CacheEvict("entityCount"),
        @CacheEvict(value = "entities", key = "#forkingInstruction.repositoryId")
      })
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ') "
          + "and hasPermission(#forkingInstruction.organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
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

    EntityDao entityDao =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Entity entity = populateWithCodeSystems().apply(entityDao.toApiModel());

    if (RepositoryType.CONCEPT_REPOSITORY.equals(destinationRepo.getRepositoryType())
        && !List.of(EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT)
            .contains(entity.getEntityType()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format(
              "entityType '%s' is invalid for concept repository",
              entity.getEntityType().getValue()));

    List<Entity> origins =
        entityRepository.getDependencies(entityDao).stream()
            .map(EntityDao::toApiModel)
            .map(populateSubEntities())
            .collect(Collectors.toList());
    origins.add(entity);
    if (forkingInstruction.isCascade() && ApiModelMapper.isAbstract(entity))
      origins.addAll(getSubclasses(organisationId, repositoryId, origins.get(0).getId(), null));

    List<Entity> results = new ArrayList<>();
    Map<String, String> ids = new HashMap<>();
    for (Entity origin :
        origins.stream().sorted(ApiModelMapper::compare).collect(Collectors.toList())) {
      String oldId = origin.getId();
      Optional<EntityDao> fork =
          entityRepository.findByRepositoryIdAndOriginId(destinationRepo.getId(), origin.getId());

      if (!forkingInstruction.isUpdate() && fork.isPresent()) continue;

      if (forkingInstruction.isUpdate() && fork.isPresent()) {
        if (fork.get().getCurrentVersion().getEquivalentEntityVersions().stream()
            .anyMatch(
                e ->
                    e.getEntity().getId().equals(origin.getId())
                        && e.getVersion().equals(origin.getVersion()))) continue;
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
        } else if (origin instanceof Concept) {
          ((Concept) origin)
              .setSuperConcepts(
                  fork.get().getSuperEntities().stream()
                      .map(e -> ((SingleConcept) new SingleConcept().id(e.getId())))
                      .collect(Collectors.toList()));
        }
      }

      if (!forkingInstruction.isUpdate() || fork.isEmpty()) {
        origin.setId(UUID.randomUUID().toString());
        origin.setVersion(1);
        if (origin instanceof Phenotype) ((Phenotype) origin).setSuperCategories(null);
        else if (origin instanceof Category) ((Category) origin).setSuperCategories(null);
        else if (origin instanceof Concept) ((Concept) origin).setSuperConcepts(null);
      }

      ids.put(oldId, origin.getId());

      if (origin instanceof Phenotype) {
        Phenotype phenotype = (Phenotype) origin;
        if (phenotype.getSuperPhenotype() != null) {
          Optional<EntityDao> superClass =
              entityRepository.findByRepositoryIdAndOriginId(
                  destinationRepo.getId(), phenotype.getSuperPhenotype().getId());
          if (superClass.isEmpty()) continue;
          phenotype.setSuperPhenotype((Phenotype) new Phenotype().id(superClass.get().getId()));
          ids.put(phenotype.getSuperPhenotype().getId(), superClass.get().getId());
        }

        if (ApiModelMapper.isAbstract(origin) && ((Phenotype) origin).getExpression() != null) {
          ((Phenotype) origin)
              .expression(
                  ApiModelMapper.replaceEntityIds(((Phenotype) origin).getExpression(), ids));
        }
      }

      if (origin.getVersion() == 1) {
        Entity curFork =
            createEntity(forkingInstruction.getOrganisationId(), destinationRepo.getId(), origin);
        curFork.addEquivalentEntitiesItem(
            new Entity().id(oldId).entityType(origin.getEntityType()).version(origin.getVersion()));
        results.add(curFork);
        entityRepository.setFork(origin.getId(), oldId);
      } else {
        Entity curFork =
            updateEntityById(
                forkingInstruction.getOrganisationId(),
                destinationRepo.getId(),
                origin.getId(),
                origin,
                null);
        curFork.addEquivalentEntitiesItem(
            new Entity().id(oldId).entityType(origin.getEntityType()).version(origin.getVersion()));
        results.add(curFork);
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
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public void deleteEntity(String organisationId, String repositoryId, String id, Boolean cascade) {
    getRepository(organisationId, repositoryId);

    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (entity.getSubEntities() != null) {
      if (ApiModelMapper.isAbstract(entity.getEntityType())) {
        entityRepository.deleteAll(entity.getSubEntities());
      } else if (ApiModelMapper.canHaveSubs(entity.getEntityType())) {
        if (cascade != null && cascade) {
          entity
              .getSubEntities()
              .forEach(e -> deleteEntity(organisationId, repositoryId, e.getId(), true));
        } else {
          for (EntityDao subEntity : entity.getSubEntities()) {
            subEntity
                .removeSuperEntitiesItem(entity)
                .addAllSuperEntitiesItems(entity.getSuperEntities());
            entityRepository.save(subEntity);
          }
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
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
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

  public Page<Entity> getEntities(
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
            repositoryIds,
            includePrimary,
            name,
            type,
            dataType,
            itemType,
            userService.getCurrentUser(),
            pageRequest)
        .map(EntityDao::toApiModel)
        .map(populateSubEntities())
        .map(populateWithCodeSystems());
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
  public Page<Entity> getEntitiesByRepositoryId(
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
        .map(populateSubEntities())
        .map(populateWithCodeSystems());
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
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
              .repository(origin.getRepository().toApiModel())
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
                    .author(
                        f.getCurrentVersion().getAuthor() != null
                            ? f.getCurrentVersion().getAuthor().getUsername()
                            : null)
                    .createdAt(f.getCurrentVersion().getCreatedAt())
                    .repository(repository.toApiModel());
              })
          .forEach(forkingStats::addForksItem);
    return forkingStats;
  }

  @Cacheable(
      value = "entities",
      key = "#repositoryId",
      condition = "#name == null && #type == null && #dataType == null")
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
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
    return entityRepository
        .findAllByRepositoryIdAndSuperEntitiesEmpty(repositoryId, Sort.by(EntityDao_.ID))
        .map(EntityDao::toApiModel)
        .map(populateSubEntities())
        .map(populateWithCodeSystems())
        .getContent();
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
  public List<Entity> getSubclasses(
      String organisationId, String repositoryId, String id, List<String> include) {
    RepositoryDao repoDao = getRepository(organisationId, repositoryId);
    EntityRepository repo = categoryRepository;
    if (RepositoryType.CONCEPT_REPOSITORY.equals(repoDao.getRepositoryType())) {
      repo = conceptRepository;
    }
    return repo.findAllByRepositoryIdAndSuperEntities_Id(repositoryId, id, Sort.by(EntityDao_.ID))
        .map(EntityDao::toApiModel)
        .map(populateSubEntities())
        .map(populateWithCodeSystems())
        .getContent();
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
  public List<Entity> getVersions(
      String organisationId, String repositoryId, String id, List<String> include) {
    getRepository(organisationId, repositoryId);
    return entityVersionRepository
        .findAllByEntity_RepositoryIdAndEntityIdOrderByVersionDesc(repositoryId, id)
        .stream()
        .map(EntityVersionDao::toApiModel)
        .map(populateWithCodeSystems())
        .collect(Collectors.toList());
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
  public Entity loadEntity(String organisationId, String repositoryId, String id, Integer version) {
    getRepository(organisationId, repositoryId);
    if (version == null)
      return entityRepository
          .findByIdAndRepositoryId(id, repositoryId)
          .map(EntityDao::toApiModel)
          .map(populateWithCodeSystems())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return entityVersionRepository
        .findByEntity_RepositoryIdAndEntityIdAndVersion(repositoryId, id, version)
        .map(EntityVersionDao::toApiModel)
        .map(populateSubEntities())
        .map(populateWithCodeSystems())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @CacheEvict(value = "entities", key = "#repositoryId")
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
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

    return populateWithCodeSystems()
        .andThen(populateSubEntities())
        .apply(entityRepository.save(entity.currentVersion(entityVersion)).toApiModel());
  }

  @CacheEvict(value = "entities", key = "#repositoryId")
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
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
      List<? extends Entity> superEntities =
          data instanceof Concept
              ? ((Concept) data).getSuperConcepts()
              : ((Category) data).getSuperCategories();
      setSuperEntities(entity, superEntities);
    }

    return mergeCodeDuplicates()
        .andThen(populateWithCodeSystems())
        .andThen(populateSubEntities())
        .apply(entityRepository.save(entity.currentVersion(newVersion)).toApiModel());
  }

  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#repositoryId, 'care.smith.top.backend.model.jpa.RepositoryDao', 'READ')")
  public ByteArrayOutputStream exportRepository(
      String organisationId, String repositoryId, String converter) {
    RepositoryDao repository = getRepository(organisationId, repositoryId);
    Entity[] entities =
        entityRepository
            .findAllByRepositoryId(repositoryId, Pageable.unpaged())
            .map(EntityDao::toApiModel)
            .map(populateWithCodeSystems())
            .stream()
            .toArray(Entity[]::new);

    Optional<Class<? extends PhenotypeExporter>> optional =
        getPhenotypeExporterImplementations().stream()
            .filter(c -> c.getSimpleName().equals(converter))
            .findFirst();

    if (optional.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, String.format("No converter '%s' available.", converter));

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      PhenotypeExporter exporter = optional.get().getConstructor().newInstance();
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
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public void importRepository(
      String organisationId, String repositoryId, String converter, InputStream stream) {

    Optional<Class<? extends PhenotypeImporter>> optional =
        getPhenotypeImporterImplementations().stream()
            .filter(c -> c.getSimpleName().equals(converter))
            .findFirst();

    if (optional.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.NOT_ACCEPTABLE, String.format("No converter '%s' available.", converter));

    try {
      PhenotypeImporter importer = optional.get().getConstructor().newInstance();
      List<Entity> entities = List.of(importer.read(stream));
      createEntities(organisationId, repositoryId, entities, null);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Import failed with error: " + e.getMessage());
    }
  }

  @CacheEvict(value = "entities", key = "#repositoryId")
  @PreAuthorize(
      "hasRole('ADMIN') or hasPermission(#organisationId, 'care.smith.top.backend.model.jpa.OrganisationDao', 'WRITE')")
  public Entity moveEntity(
      String organisationId, String repositoryId, String entityId, List<Entity> superEntities) {
    getRepository(organisationId, repositoryId);
    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(entityId, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!ApiModelMapper.isRestricted(entity.getEntityType())) {
      setSuperEntities(entity, superEntities);
    }

    return populateWithCodeSystems()
        .andThen(populateSubEntities())
        .apply(entityRepository.save(entity).toApiModel());
  }

  public Set<Class<? extends PhenotypeExporter>> getPhenotypeExporterImplementations() {
    return getPluginReflections().getSubTypesOf(PhenotypeExporter.class);
  }

  public Set<Class<? extends PhenotypeImporter>> getPhenotypeImporterImplementations() {
    return getPluginReflections().getSubTypesOf(PhenotypeImporter.class);
  }

  private Reflections getPluginReflections() {
    return new Reflections(new ConfigurationBuilder().forPackage(PLUGIN_PACKAGE));
  }

  /**
   * Modifies entity dao's super entities in place.
   *
   * @param entityDao Entity dao of which super entities are updated
   * @param superEntities Super entities to apply to the entity dao
   */
  private void setSuperEntities(EntityDao entityDao, List<? extends Entity> superEntities) {
    entityDao.superEntities(null);
    if (superEntities != null)
      for (Entity superEntity : superEntities)
        entityRepository
            .findByIdAndRepositoryId(superEntity.getId(), entityDao.getRepository().getId())
            .ifPresent(entityDao::addSuperEntitiesItem);
  }

  /**
   * Create an entity, if the entity depends on other entities, depending on the entity type the
   * following will happen:
   *
   * <ul>
   *   <li>category/concept: super categories/concepts are created too
   *   <li>abstract phenotype: entities referenced in expressions are created too
   *   <li>restriction: super phenotypes are created too
   * </ul>
   *
   * If you are calling this method multiple times, you should sort the entities with the {@link
   * ApiModelMapper#compare(Entity, Entity)} comparator.
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
    } else if (ApiModelMapper.isConcept(data)) {
      if (((Concept) data).getSuperConcepts() != null)
        ((Concept) data)
            .setSuperConcepts(
                ((Concept) data)
                    .getSuperConcepts().stream()
                        .map(
                            e ->
                                (SingleConcept)
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

    if (data instanceof Category && ((Category) data).getSuperCategories() != null) {
      ((Category) data)
          .superCategories(
              ((Category) data)
                  .getSuperCategories().stream()
                      .map(
                          c ->
                              (Category)
                                  new Category()
                                      .id(ids.get(c.getId()))
                                      .entityType(EntityType.CATEGORY))
                      .collect(Collectors.toList()));
    }

    if (data instanceof Concept && ((Concept) data).getSuperConcepts() != null) {
      ((Concept) data)
          .superConcepts(
              ((Concept) data)
                  .getSuperConcepts().stream()
                      .map(
                          c ->
                              (SingleConcept)
                                  new SingleConcept()
                                      .id(ids.get(c.getId()))
                                      .entityType(EntityType.SINGLE_CONCEPT))
                      .collect(Collectors.toList()));
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
        && !List.of(EntityType.SINGLE_CONCEPT, EntityType.COMPOSITE_CONCEPT)
            .contains(data.getEntityType()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          String.format(
              "entityType '%s' is invalid for concept repository",
              data.getEntityType().getValue()));

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

    if (data instanceof Concept && ((Concept) data).getSuperConcepts() != null)
      for (Concept concept : ((Concept) data).getSuperConcepts())
        conceptRepository
            .findByIdAndRepositoryId(concept.getId(), repositoryId)
            .ifPresent(entity::addSuperEntitiesItem);

    entity = entityRepository.save(entity);
    EntityVersionDao entityVersion =
        entityVersionRepository.save(new EntityVersionDao(data).version(1).entity(entity));
    entity.currentVersion(entityVersion);

    return mergeCodeDuplicates()
        .andThen(populateWithCodeSystems())
        .andThen(populateSubEntities())
        .apply(entityRepository.save(entity).toApiModel());
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

  /**
   * Checks if the entity has sub categories.
   *
   * @param entity The entity to be checked.
   * @return true, if provided entity has sub categories.
   */
  private boolean hasSubCategories(@NotNull Entity entity) {
    return hasSubEntities(entity, Collections.singletonList(EntityType.CATEGORY));
  }

  /**
   * Checks if the entity has sub phenotypes.
   *
   * @param entity The entity to be checked.
   * @return true, if provided entity has sub phenotypes.
   */
  private boolean hasSubPhenotypes(@NotNull Entity entity) {
    return hasSubEntities(entity, ApiModelMapper.phenotypeTypes());
  }

  /**
   * Checks if the entity has sub concepts.
   *
   * @param entity The entity to be checked
   * @return true, if provided entity has sub concepts
   */
  private boolean hasSubConcepts(@NotNull Entity entity) {
    return hasSubEntities(entity, ApiModelMapper.conceptTypes());
  }

  /**
   * Checks if the entity has sub entities of the provided entity types
   *
   * @param entity The entity to be checked.
   * @param entityTypes The entity types to check for.
   * @return true, if provided entity has sub entities.
   */
  private boolean hasSubEntities(@NotNull Entity entity, Collection<EntityType> entityTypes) {
    return entityRepository.existsByIdAndSubEntities_EntityTypeIn(entity.getId(), entityTypes);
  }

  /**
   * Checks if the provided entity has sub categories, phenotypes or concepts. If this is not the
   * case, the respective fields are initialised with empty arrays to indicate absence of sub
   * entities.
   *
   * @return The provided entity instance with modified fields.
   */
  private Function<Entity, Entity> populateSubEntities() {
    return e -> {
      if (e instanceof Category) {
        if (!hasSubCategories(e)) ((Category) e).setSubCategories(new ArrayList<>());
        if (!hasSubPhenotypes(e)) ((Category) e).setPhenotypes(new ArrayList<>());
      } else if (e instanceof SingleConcept) {
        if (!hasSubConcepts(e)) ((SingleConcept) e).setSubConcepts(new ArrayList<>());
      }
      return e;
    };
  }

  private Function<Entity, Entity> populateWithCodeSystems() {
    return e -> {
      if (e.getCodes() == null) return e;
      return e.codes(
          e.getCodes().stream().peek(this::populateWithCodeSystems).collect(Collectors.toList()));
    };
  }

  private void populateWithCodeSystems(Code c) {
    codeRepository.getCodeSystem(c.getCodeSystem().getUri()).ifPresent(c::codeSystem);
    Optional.ofNullable(c.getChildren()).stream()
        .flatMap(Collection::stream)
        .forEach(this::populateWithCodeSystems);
  }

  private Function<Entity, Entity> mergeCodeDuplicates() {
    return e -> e.codes(getMergedCodes(e));
  }

  private List<Code> getMergedCodes(Entity entity) {
    Graph<CodeVertexAttributes, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    Map<URI, CodeVertexAttributes> vertexCache = new HashMap<>();
    entity.getCodes().stream()
        .map(code -> getGraph(code, vertexCache))
        .forEach(g -> Graphs.addGraph(graph, g));

    ConnectivityInspector<CodeVertexAttributes, DefaultEdge> ci =
        new ConnectivityInspector<>(graph);
    try {
      ci.connectedSets().stream()
          .map(vertices -> new AsSubgraph<>(graph, vertices))
          .forEach(
              subGraph ->
                  subGraph
                      .outgoingEdgesOf(
                          roots(subGraph).findFirst().orElseThrow(NoSuchElementException::new))
                      .stream()
                      .flatMap(edge -> getSuperfluousEdges(subGraph, edge, 1))
                      .collect(Collectors.toList())
                      .forEach(graph::removeEdge));
    } catch (NoSuchElementException e) {
      LOGGER.warning("Unable to find code tree root for entity " + entity.getId());
      return entity.getCodes();
    }

    return roots(graph).map(root -> assembleCodeTree(graph, root)).collect(Collectors.toList());
  }

  // helper class for code duplicate resolution
  private static class CodeVertexAttributes {
    Code code;
    int depth = 0;
    DefaultEdge bestIncomingEdge;

    CodeVertexAttributes(@NotNull Code code) {
      this.code = code;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CodeVertexAttributes)) return false;
      return code.getUri().equals(((CodeVertexAttributes) obj).code.getUri());
    }

    @Override
    public int hashCode() {
      return code.getUri().hashCode();
    }
  }

  // helper method for code duplicate resolution
  private Graph<CodeVertexAttributes, DefaultEdge> getGraph(
      Code code, Map<URI, CodeVertexAttributes> vertexCache) {
    Graph<CodeVertexAttributes, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    CodeVertexAttributes attributes = getVertex(code, vertexCache);
    graph.addVertex(attributes);
    fillInChildren(graph, code, vertexCache);
    return graph;
  }

  private CodeVertexAttributes getVertex(Code code, Map<URI, CodeVertexAttributes> vertexCache) {
    if (vertexCache.containsKey(code.getUri())) {
      return vertexCache.get(code.getUri());
    }
    CodeVertexAttributes attributes = new CodeVertexAttributes(code);
    vertexCache.put(code.getUri(), attributes);
    return attributes;
  }

  // helper method for code duplicate resolution
  private void fillInChildren(
      Graph<CodeVertexAttributes, DefaultEdge> graph,
      Code parentCode,
      Map<URI, CodeVertexAttributes> vertexCache) {
    parentCode
        .getChildren()
        .forEach(
            child -> {
              CodeVertexAttributes childAttributes = getVertex(child, vertexCache);
              graph.addVertex(childAttributes);
              graph.addEdge(getVertex(parentCode, vertexCache), childAttributes);
              fillInChildren(graph, child, vertexCache);
            });
  }

  // helper method for code duplicate resolution
  private Stream<CodeVertexAttributes> roots(Graph<CodeVertexAttributes, DefaultEdge> graph) {
    return graph.vertexSet().stream().filter(vertex -> graph.incomingEdgesOf(vertex).isEmpty());
  }

  // helper method for code duplicate resolution
  private Stream<DefaultEdge> getSuperfluousEdges(
      Graph<CodeVertexAttributes, DefaultEdge> graph, DefaultEdge incoming, int depth) {
    CodeVertexAttributes node = graph.getEdgeTarget(incoming);
    if (node.depth == 0) {
      // first visit of this node
      node.bestIncomingEdge = incoming;
      node.depth = depth;
      return graph.outgoingEdgesOf(node).stream()
          .flatMap(outgoing -> getSuperfluousEdges(graph, outgoing, depth + 1));
    }
    if (depth > node.depth) {
      // This path is better. Replace bestIncomingEdge and return the old one for deletion.
      DefaultEdge worseEdge = node.bestIncomingEdge;
      node.bestIncomingEdge = incoming;
      node.depth = depth;
      return Stream.of(worseEdge);
    } else {
      // This path is worse. Return it for deletion.
      return Stream.of(incoming);
    }
  }

  private Code assembleCodeTree(
      Graph<CodeVertexAttributes, DefaultEdge> graph, CodeVertexAttributes vertex) {
    return vertex.code.children(
        graph.outgoingEdgesOf(vertex).stream()
            .map(graph::getEdgeTarget)
            .map(childVertex -> assembleCodeTree(graph, childVertex))
            .collect(Collectors.toList()));
  }
}
