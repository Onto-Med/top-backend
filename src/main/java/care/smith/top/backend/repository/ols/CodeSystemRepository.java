package care.smith.top.backend.repository.ols;

import care.smith.top.backend.service.ols.OLSOntologiesResponse;
import care.smith.top.model.CodeSystem;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class CodeSystemRepository extends OlsRepository {
  @Cacheable("olsOntologies")
  public Map<String, CodeSystem> getAllCodeSystems() {
    OLSOntologiesResponse response =
        terminologyService
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/ontologies")
                        .queryParam("page", 0)
                        .queryParam("size", 1000)
                        .build())
            .retrieve()
            .bodyToMono(OLSOntologiesResponse.class)
            .block();

    return Arrays.stream(Objects.requireNonNull(response).get_embedded().getOntologies())
        .map(
            ontology ->
                new CodeSystem()
                    .externalId(ontology.getOntologyId())
                    .uri(ontology.getConfig().getId())
                    .name(ontology.getConfig().getTitle())
                    .shortName(ontology.getConfig().getPreferredPrefix()))
        .collect(Collectors.toMap(CodeSystem::getExternalId, Function.identity()));
  }
}
