package care.smith.top.backend.repository.ols;

import care.smith.top.backend.service.ols.OLSSuggestResponse;
import care.smith.top.backend.service.ols.OLSSuggestResponseBody;
import care.smith.top.backend.service.ols.OLSTerm;
import care.smith.top.model.Code;
import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class CodeRepository extends OlsRepository {
  @Value("${spring.paging.page-size:10}")
  private int suggestionsPageSize;

  @Autowired private CodeSystemRepository codeSystemRepository;

  /**
   * This method converts a TOP page number to OLS page number. OLS page count starts from 0, we
   * start from 1. Also, OLS page numbers are indices of items, where the page starts.
   *
   * @param page TOP page number to be converted to OLS page number.
   * @return OLS page number
   */
  public static int toOlsPage(Integer page, int pageSize) {
    return page == null ? 0 : (page - 1) * pageSize;
  }

  public Code getCode(URI uri, String codeSystemId) {
    CodeSystem codeSystem =
        getCodeSystem(codeSystemId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Code system '%s' does not exist", codeSystemId)));

    OLSTerm term =
        terminologyService
            .get()
            .uri(
                uriBuilder ->
                    // OLS requires that uri is double URL encoded, so we encode it once and
                    // additionally rely on uriBuilder encoding it for a second time
                    uriBuilder
                        .path("/term")
                        .pathSegment(
                            codeSystemId, URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8))
                        .build())
            .retrieve()
            .bodyToMono(OLSTerm.class)
            .onErrorResume(
                WebClientResponseException.class,
                e ->
                    e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()
                        ? Mono.empty()
                        : Mono.error(e))
            .block();

    if (term == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          String.format("Code could not be found in terminology '%s'.", codeSystemId));

    return new Code()
        .code(term.getLabel())
        .uri(URI.create(term.getIri()))
        .codeSystem(codeSystem)
        .synonyms(term.getSynonyms());
  }

  public CodePage getCodes(
      String term, List<String> codeSystemIds, Integer page, SEARCH_METHOD searchMethod) {
    OLSSuggestResponseBody response =
        Objects.requireNonNull(
                terminologyService
                    .get()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path(searchMethod.getEndpoint())
                                .queryParam("q", term)
                                .queryParam("fieldList", "id,iri,ontology_name,label,synonym")
                                .queryParam("start", toOlsPage(page, suggestionsPageSize))
                                .queryParam("rows", suggestionsPageSize)
                                .queryParam(
                                    "ontology",
                                    codeSystemIds == null ? "" : String.join(",", codeSystemIds))
                                .build())
                    .retrieve()
                    .bodyToMono(OLSSuggestResponse.class)
                    .block())
            .getResponse();

    int totalPages = (int) Math.ceil((double) response.getNumFound() / suggestionsPageSize);
    List<Code> content =
        Arrays.stream(response.getDocs())
            .map(
                responseItem -> {
                  String primaryLabel =
                      responseItem.getSynonym() != null && responseItem.getSynonym().size() != 0
                          ? responseItem.getSynonym().get(0)
                          : null;

                  return new Code()
                      .code(responseItem.getLabel())
                      .name(primaryLabel)
                      .uri(responseItem.getIri())
                      .code(responseItem.getLabel())
                      .uri(responseItem.getIri())
                      .synonyms(
                          responseItem.getSynonym() != null
                              ? Set.copyOf(responseItem.getSynonym()).stream()
                                  .distinct()
                                  .filter(synonym -> !synonym.equals(primaryLabel))
                                  .collect(Collectors.toList())
                              : null)
                      .highlightLabel(
                          responseItem.getAutoSuggestion().getLabel_autosuggest() != null
                                  && responseItem.getAutoSuggestion().getLabel_autosuggest().size()
                                      != 0
                              ? responseItem.getAutoSuggestion().getLabel_autosuggest().get(0)
                              : null)
                      .highlightSynonym(
                          responseItem.getAutoSuggestion().getSynonym_autosuggest() != null
                                  && responseItem
                                          .getAutoSuggestion()
                                          .getSynonym_autosuggest()
                                          .size()
                                      != 0
                              ? responseItem.getAutoSuggestion().getSynonym_autosuggest().get(0)
                              : null)
                      .codeSystem(getCodeSystem(responseItem.getOntology_name()).orElse(null));
                })
            .collect(Collectors.toList());

    return (CodePage)
        new CodePage()
            .content(content)
            .size(suggestionsPageSize)
            .totalElements((long) response.getNumFound())
            .number(page)
            .totalPages(totalPages);
  }

  private Optional<CodeSystem> getCodeSystem(String externalId) {
    if (codeSystemRepository.getAllCodeSystems().containsKey(externalId))
      return Optional.of(codeSystemRepository.getAllCodeSystems().get(externalId));
    return Optional.empty();
  }
}
