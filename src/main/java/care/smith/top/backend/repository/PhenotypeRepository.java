package care.smith.top.backend.repository;

import care.smith.top.backend.model.Phenotype;

import java.util.List;

@org.springframework.stereotype.Repository
public interface PhenotypeRepository extends EntityBaseRepository<Phenotype> {
  List<Phenotype> findAllByRepositoryIdAndSuperPhenotypeId(String repositoryId, String superPhenotypeId);
}
