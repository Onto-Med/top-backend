package care.smith.top.backend.repository;

import care.smith.top.backend.model.Entity;
import care.smith.top.backend.model.EntityType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface EntityRepository extends PagingAndSortingRepository<Entity, String> {
  long countByEntityType(EntityType[] entityTypes);

  Optional<Entity> findByIdAndRepositoryId(String id, String repositoryId);

  Stream<Entity> findBySuperPhenotypeId(String superPhenotypeId);

  Collection<Entity> findAllByRepositoryIdAndSuperPhenotypeId(
      String repositoryId, String superPhenotypeId);

  Collection<Entity> findAllById(String id, PageRequest pageRequest);

  Optional<Entity> findByIdAndRepositoryIdAndVersion(
      String id, String repositoryId, Integer version);
}
