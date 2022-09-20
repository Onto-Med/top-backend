package care.smith.top.backend.repository;

import care.smith.top.backend.model.DataType;
import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.EntityType;
import care.smith.top.backend.model.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@org.springframework.stereotype.Repository
public interface EntityRepository extends PagingAndSortingRepository<Entity, String> {
  long countByEntityType(EntityType[] entityTypes);

  Stream<Entity> findBySuperPhenotype(String superPhenotypeId);

  Collection<Entity> findAllByRepositoryIdAndSuperPhenotypeId(
      String repositoryId, String superPhenotypeId);

  Collection<Entity> findAllById(String id, PageRequest pageRequest);

  Optional<Entity> findByIdAndRepositoryIdAndVersion(
      String id, String repositoryId, Integer version);

  Optional<Entity> findCurrentById(String id);

  Optional<Entity> getPrevious(Entity e);

  Integer getNextVersion(Entity oldEntity);

  Optional<Entity> getNext(Entity e);

  void setCurrent(Entity entity);

  Optional<Entity> findOrigin(Entity entity);

  Collection<Entity> findAllByRepositoryIdAndNameAndEntityTypeAndDataTypeAndPrimary(
      String repositoryId,
      String name,
      List<EntityType> type,
      DataType dataType,
      boolean primary,
      PageRequest pageRequest);

  void setPreviousVersion(Entity entity, Entity prev);

  Optional<Entity> getFork(Entity origin, Repository repository);

  void setFork(String forkId, String originId);
}
