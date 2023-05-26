package care.smith.top.backend.service;

import care.smith.top.backend.service.ols.OLSOntologiesResponse;
import care.smith.top.backend.service.ols.OLSSuggestResponse;
import care.smith.top.backend.service.ols.OLSSuggestResponseBody;
import care.smith.top.backend.service.ols.OLSTerm;
import care.smith.top.model.Code;
import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystem;
import care.smith.top.model.CodeSystemPage;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author ralph
 */
@Service
public class OLSCodeService {

  private WebClient terminologyService;

  @Value("${spring.paging.page-size:10}")
  private int suggestionsPageSize;

  @Value("${spring.paging.page-size:10}")
  private int ontologyPageSize;

  @NotNull
  private static Predicate<CodeSystem> filterByName(String name) {
    return cs ->
        name == null
            || cs.getName() != null && StringUtils.containsIgnoreCase(cs.getName(), name)
            || cs.getShortName() != null && StringUtils.containsIgnoreCase(cs.getShortName(), name);
  }

  /**
   * This method converts a TOP page number to OLS page number. OLS page count starts from 0, we
   * start from 1.
   *
   * @param page TOP page number to be converted to OLS page number.
   * @return OLS page number
   */
  private static int toOlsPage(Integer page) {
    return page == null ? 0 : page - 1;
  }

  @Autowired
  public void setTerminologyServiceEndpoint(
      @Value("${coding.terminology-service}") String terminologyServiceEndpoint) {
    int size = 16 * 1024 * 1024;
    ExchangeStrategies exchangeStrategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();
    terminologyService =
        WebClient.builder()
            .baseUrl(terminologyServiceEndpoint)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchangeStrategies(exchangeStrategies)
            .build();
  }

  public Code getCode(URI uri, String codeSystemId, List<String> include, Integer page) {
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
      List<String> include, String label, List<String> codeSystemIds, Integer page) {
    return getCodes(include, label, codeSystemIds, page, SEARCH_METHOD.SEARCH);
  }

  public CodePage getCodeSuggestions(
      List<String> include, String label, List<String> codeSystemIds, Integer page) {
    return getCodes(include, label, codeSystemIds, page, SEARCH_METHOD.SUGGEST);
  }

  /**
   * This method searches for code systems based on uri or name.
   *
   * <p>OLS3 has no parameter to filter ontologies. Thus, to not break paging, all ontologies are
   * loaded at once and filtered locally.
   *
   * @param include unused
   * @param uri Code system URI to filter for
   * @param name Code system name to filter for
   * @param page Requested page
   * @return A {@link CodeSystemPage} containing matching code systems.
   */
  public CodeSystemPage getCodeSystems(List<String> include, URI uri, String name, Integer page) {
    int requestedPage = page == null ? 1 : page;
    int skipCount = (requestedPage - 1) * ontologyPageSize;

    List<CodeSystem> allCodeSystems = getAllCodeSystems();
    List<CodeSystem> filteredCodeSystems =
        allCodeSystems.stream()
            .filter(cs -> uri == null || uri.equals(cs.getUri()))
            .filter(filterByName(name))
            .collect(Collectors.toList());
    List<CodeSystem> content =
        filteredCodeSystems.stream()
            .skip(skipCount)
            .limit(ontologyPageSize)
            .collect(Collectors.toList());

    return (CodeSystemPage)
        new CodeSystemPage()
            .content(content)
            .size(ontologyPageSize)
            .totalElements((long) filteredCodeSystems.size())
            .number(requestedPage)
            .totalPages(filteredCodeSystems.size() / ontologyPageSize + 1);
  }

  @Cacheable("olsOntologies")
  public List<CodeSystem> getAllCodeSystems() {
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
        .sorted((a, b) -> a.getExternalId().compareToIgnoreCase(b.getExternalId()))
        .collect(Collectors.toList());
  }

  private CodePage getCodes(
      List<String> include,
      String term,
      List<String> codeSystemIds,
      Integer page,
      SEARCH_METHOD searchMethod) {
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
                                .queryParam("start", toOlsPage(page))
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
    return getAllCodeSystems().stream()
        .filter(cs -> cs.getExternalId().equals(externalId))
        .findFirst();
  }

  private enum SEARCH_METHOD {
    SEARCH("/search"),
    SUGGEST("/select");

    private String endpoint;

    SEARCH_METHOD(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }
  }
}
