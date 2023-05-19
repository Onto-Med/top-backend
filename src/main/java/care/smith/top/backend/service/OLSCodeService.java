package care.smith.top.backend.service;

import care.smith.top.backend.service.ols.OLSOntologiesResponse;
import care.smith.top.backend.service.ols.OLSSuggestResponse;
import care.smith.top.backend.service.ols.OLSSuggestResponseBody;
import care.smith.top.backend.service.ols.OLSTerm;
import care.smith.top.model.Code;
import care.smith.top.model.CodePage;
import care.smith.top.model.CodeSystem;
import care.smith.top.model.CodeSystemPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ralph
 */
@Service
public class OLSCodeService {

  private WebClient terminologyService;

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

  @Autowired
  public void setTerminologyServiceEndpoint(
      @Value("${coding.terminology-service}") String terminologyServiceEndpoint) {
    terminologyService =
        WebClient.builder()
            .baseUrl(terminologyServiceEndpoint)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Value("10")
  private int suggestionsPageSize;

  @Value("10")
  private int ontologyPageSize;

  public Code getCode(URI uri, String codeSystemId, List<String> include, Integer page) {
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
            .block();

    if (term == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          String.format("Code could not be found in terminology '%s'.", codeSystemId));

    CodeSystem codeSystem =
        new CodeSystem()
            .uri(URI.create(term.getOntology_iri()))
            .name(term.getOntology_prefix())
            .externalId(term.getOntology_name());

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
                  var uniqueLabels = new HashSet<String>();
                  switch (searchMethod) {
                    case SEARCH:
                      uniqueLabels.addAll(
                          Optional.ofNullable(responseItem.getAutoSuggestion().getLabel())
                              .orElse(Collections.emptyList()));
                      uniqueLabels.addAll(
                          Optional.ofNullable(responseItem.getAutoSuggestion().getSynonym())
                              .orElse(Collections.emptyList()));
                      break;
                    case SUGGEST:
                      uniqueLabels.addAll(
                          Optional.ofNullable(
                                  responseItem.getAutoSuggestion().getLabel_autosuggest())
                              .orElse(Collections.emptyList()));
                      uniqueLabels.addAll(
                          Optional.ofNullable(
                                  responseItem.getAutoSuggestion().getSynonym_autosuggest())
                              .orElse(Collections.emptyList()));
                      break;
                  }
                  return new Code()
                      .code(responseItem.getLabel())
                      .uri(responseItem.getIri())
                      .code(responseItem.getLabel())
                      .uri(responseItem.getIri())
                      .synonyms(new ArrayList<>(uniqueLabels));
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

  public CodeSystemPage getCodeSystems(List<String> include, URI uri, String name, Integer page) {
    OLSOntologiesResponse response =
        terminologyService
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/ontologies")
                        .queryParam("page", toOlsPage(page))
                        .queryParam("size", ontologyPageSize)
                        .build())
            .retrieve()
            .bodyToMono(OLSOntologiesResponse.class)
            .block();

    // OLS has no filtering option in its ontologies endpoint, so we have to filter here.
    List<CodeSystem> content =
        Arrays.stream(Objects.requireNonNull(response).get_embedded().getOntologies())
            .filter(ontology -> uri == null || ontology.getConfig().getId().equals(uri))
            .filter(ontology -> name == null || ontology.getConfig().getTitle().equals(name))
            .map(
                ontology ->
                    new CodeSystem()
                        .externalId(ontology.getOntologyId())
                        .uri(ontology.getConfig().getId())
                        .name(ontology.getConfig().getTitle()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());

    // TODO: page size, totalElements and page number are wrong because we filtered afterwards.
    // Actually, there is no way of finding out the total number of filtered elements.
    return (CodeSystemPage)
        new CodeSystemPage()
            .content(content)
            .size(response.getPage().getSize())
            .totalElements(response.getPage().getTotalElements())
            .number(response.getPage().getNumber())
            .totalPages(response.getPage().getTotalPages());
  }

  /**
   * This method converts a TOP page number to OLS page number. OLS page count starts from 0, we
   * start from 1.
   *
   * @param page TOP page number to be converted to OLS page number.
   * @return OLS page number
   */
  private int toOlsPage(Integer page) {
    return page == null ? 0 : page - 1;
  }
}
