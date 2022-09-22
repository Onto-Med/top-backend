package care.smith.top.backend.repository;

import care.smith.top.backend.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@org.springframework.stereotype.Repository
public interface EntityRepository extends PagingAndSortingRepository<Entity, String> {
  long countByEntityType(EntityType[] entityTypes);

  List<Entity> findAllBySuperPhenotypeId(String superPhenotypeId);

  Collection<Entity> findAllById(String id, PageRequest pageRequest);

  Optional<Entity> findByIdAndVersion(String id, Integer version);

  default Optional<Entity> findCurrentById(String id) {
    return Optional.empty();
  }

//  Collection<Entity> findAllByRepositoryIdAndNameAndEntityTypeAndDataTypeAndPrimary(
//      String repositoryId,
//      String name,
//      List<EntityType> type,
//      DataType dataType,
//      boolean primary,
//      PageRequest pageRequest);

  default Optional<Entity> getFork(Entity origin, Repository repository) {
    return origin.getForks().stream()
        .filter(e -> e.getRepository().equals(repository) && e.getNextVersion() == null)
        .findFirst();
  }

  default void setFork(String forkId, String originId) {

  }
}
