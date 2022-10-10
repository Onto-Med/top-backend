package care.smith.top.backend.service;

import care.smith.top.backend.model.EntityDao;
import care.smith.top.backend.model.EntityVersionDao;
import care.smith.top.backend.model.LocalisableTextDao;
import care.smith.top.backend.model.RepositoryDao;
import care.smith.top.backend.repository.*;
import care.smith.top.model.*;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.phenotype2r.Phenotype2RConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class EntityService implements ContentService {
  @Value("${spring.paging.page-size:10}")
  private int pageSize;

  @Autowired private EntityRepository entityRepository;
  @Autowired private EntityVersionRepository entityVersionRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private PhenotypeRepository phenotypeRepository;
  @Autowired private RepositoryService repositoryService;

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

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
  public Entity createEntity(String organisationId, String repositoryId, Entity data) {
    if (entityRepository.existsById(data.getId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    RepositoryDao repository = getRepository(organisationId, repositoryId);

    if (data.getEntityType() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityType is missing");

    EntityDao entity = new EntityDao(data).repository(repository);

    if (data instanceof Category && ((Category) data).getSuperCategories() != null)
      for (Category category : ((Category) data).getSuperCategories())
        categoryRepository
            .findByIdAndRepositoryId(category.getId(), repositoryId)
            .ifPresent(entity::addSuperEntitiesItem);

    if (data instanceof Phenotype && ((Phenotype) data).getSuperPhenotype() != null)
      phenotypeRepository
          .findByIdAndRepositoryId(((Phenotype) data).getSuperPhenotype().getId(), repositoryId)
          .ifPresent(entity::addSuperEntitiesItem);

    entity = entityRepository.save(entity);
    EntityVersionDao entityVersion =
        entityVersionRepository.save(new EntityVersionDao(data).version(1).entity(entity));
    entity.currentVersion(entityVersion);

    return entityRepository.save(entity).toApiModel();
  }

  @Caching(
      evict = {@CacheEvict("entityCount"), @CacheEvict(value = "entities", key = "#repositoryId")})
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
    if (previous != null && next != null)
      entityVersionRepository.save(next.previousVersion(previous));

    entityVersionRepository.delete(entityVersion);
  }

  public StringWriter exportEntity(
      String organisationId, String repositoryId, String id, String format, Integer version) {
    RepositoryDao repository = getRepository(organisationId, repositoryId);
    StringWriter writer = new StringWriter();
    Page<EntityDao> entities =
        entityRepository.findAllByRepositoryId(repository.getId(), Pageable.unpaged());
    EntityDao entity =
        entities.stream()
            .filter(e -> id.equals(e.getId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if ("vnd.r-project.r".equals(format)) {
      Collection<Phenotype> phenotypes =
          entities.stream()
              .filter(e -> !EntityType.CATEGORY.equals(e.getEntityType()))
              .map(EntityDao::toApiModel)
              .map(e -> (Phenotype) e)
              .collect(Collectors.toList());
      try {
        Phenotype2RConverter converter = new Phenotype2RConverter(phenotypes);
        for (EntityDao subEntity : entity.getSubEntities()) { // TODO: recursively collect entities
          converter.convert(subEntity.getId(), writer);
          writer.append(System.lineSeparator());
        }
        converter.convert(id, writer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
    }
    return writer;
  }

  public List<Entity> getEntities(
      List<String> include, String name, List<EntityType> type, DataType dataType, Integer page) {
    PageRequest pageRequest = PageRequest.of(page != null ? page - 1 : 0, pageSize);
    return phenotypeRepository
        .findAllByTitleAndEntityTypeAndDataType(name, type, dataType, pageRequest)
        .flatMap(
            entity -> {
              Stream<EntityDao> result = Stream.of(entity);
              if (type == null
                  || type.contains(ApiModelMapper.toRestrictedEntityType(entity.getEntityType()))) {
                String repositoryId = entity.getRepository().getId();
                result =
                    Stream.concat(
                        result,
                        entityRepository
                            .findAllByRepositoryIdAndSuperEntities_Id(repositoryId, entity.getId())
                            .stream());
              }
              return result;
            })
        .filter(distinctByKey(EntityDao::getId))
        .map(EntityDao::toApiModel)
        .toList();
  }

  @Cacheable(
      value = "entities",
      key = "#repositoryId",
      condition = "#name == null && #type == null && #dataType == null")
  public List<Entity> getEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      Integer page) {
    getRepository(organisationId, repositoryId);
    PageRequest pageRequest = PageRequest.of(page != null ? page - 1 : 0, pageSize);

    return phenotypeRepository
        .findAllByRepositoryIdAndTitleAndEntityTypeAndDataType(
            repositoryId, name, type, dataType, pageRequest)
        .flatMap(
            entity -> {
              Stream<EntityDao> result = Stream.of(entity);
              if (type == null
                  || type.contains(ApiModelMapper.toRestrictedEntityType(entity.getEntityType()))) {
                result =
                    Stream.concat(
                        result,
                        entityRepository
                            .findAllByRepositoryIdAndSuperEntities_Id(repositoryId, entity.getId())
                            .stream());
              }
              return result;
            })
        .filter(distinctByKey(EntityDao::getId))
        .map(EntityDao::toApiModel)
        .toList();
  }

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

  public List<Entity> getRootEntitiesByRepositoryId(
      String organisationId,
      String repositoryId,
      List<String> include,
      String name,
      List<EntityType> type,
      DataType dataType,
      Integer page) {
    getRepository(organisationId, repositoryId);
    PageRequest pageRequest = PageRequest.of(page == null ? 0 : page - 1, pageSize);
    return entityRepository
        .findAllByRepositoryIdAndSuperEntitiesEmpty(repositoryId, pageRequest)
        .map(EntityDao::toApiModel)
        .getContent();
  }

  public List<Entity> getSubclasses(
      String organisationId, String repositoryId, String id, List<String> include) {
    getRepository(organisationId, repositoryId);
    return categoryRepository.findAllByRepositoryIdAndSuperEntities_Id(repositoryId, id).stream()
        .map(EntityDao::toApiModel)
        .collect(Collectors.toList());
  }

  public List<Entity> getVersions(
      String organisationId, String repositoryId, String id, List<String> include) {
    getRepository(organisationId, repositoryId);
    return entityVersionRepository
        .findAllByEntity_RepositoryIdAndEntityIdOrderByVersionDesc(repositoryId, id)
        .stream()
        .map(EntityVersionDao::toApiModel)
        .collect(Collectors.toList());
  }

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
  public Entity updateEntityById(
      String organisationId, String repositoryId, String id, Entity data, List<String> include) {
    getRepository(organisationId, repositoryId);
    EntityDao entity =
        entityRepository
            .findByIdAndRepositoryId(id, repositoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    EntityVersionDao latestVersion = entityVersionRepository.findByEntityIdAndNextVersionNull(id);
    EntityVersionDao newVersion = new EntityVersionDao(data).entity(entity);

    if (latestVersion != null)
      newVersion.previousVersion(latestVersion).version(latestVersion.getVersion() + 1);
    else newVersion.version(0);

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

  /**
   * Get {@link Repository} by repositoryId and directoryId. If the repository does not exist or is
   * not associated with the directory, this method will throw an exception.
   *
   * @param organisationId ID of the {@link Organisation}
   * @param repositoryId ID of the {@link Repository}
   * @return The matching repository, if it exists.
   */
  private RepositoryDao getRepository(String organisationId, String repositoryId) {
    return repositoryService
        .getRepository(organisationId, repositoryId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Repository '%s' does not exist!", repositoryId)));
  }
}
