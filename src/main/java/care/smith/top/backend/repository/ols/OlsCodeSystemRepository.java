package care.smith.top.backend.repository.ols;

import care.smith.top.backend.service.ols.OLSOntologiesResponse;
import care.smith.top.model.CodeSystem;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class OlsCodeSystemRepository extends OlsRepository {
  private static final Logger LOGGER = Logger.getLogger(OlsCodeSystemRepository.class.getName());

  @Cacheable("olsOntologies")
  public Map<URI, CodeSystem> getAllCodeSystems() throws OlsConnectionException {
    try {
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
                      .uri(
                          Optional.ofNullable(ontology.getConfig().getVersionId())
                              .orElse(ontology.getConfig().getId()))
                      .name(ontology.getConfig().getTitle())
                      .shortName(ontology.getConfig().getPreferredPrefix()))
          .collect(
              Collectors.toMap(
                  CodeSystem::getUri, Function.identity(), (existing, replacement) -> existing));
    } catch (Exception e) {
      LOGGER.severe("Could not retrieve code systems from terminology server: " + e.getMessage());
      throw new OlsConnectionException(e);
    }
  }
}
