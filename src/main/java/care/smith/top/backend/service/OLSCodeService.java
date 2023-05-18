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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
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
      SEARCH_METHOD(String endpoint) {this.endpoint = endpoint;}
      public String getEndpoint() {return endpoint;}
      public void setEndpoint(String endpoint) {this.endpoint = endpoint;}
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
      OLSTerm term = terminologyService.get()
              .uri(uriBuilder -> uriBuilder
                      .path("/term").pathSegment(codeSystemId, URLEncoder.encode(uri.toString())) // OLS requires that uri is double URL encoded, so we encode it once and additionally rely on uriBuilder encoding it for a second time
                      .build()
              )
              .retrieve()
              .bodyToMono(OLSTerm.class)
              .block();

      Code result = new Code();
      result.setCode(term.getLabel());
      result.setUri(URI.create(term.getIri()));

      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setUri(URI.create(term.getOntology_iri()));
      codeSystem.setName(term.getOntology_prefix());
      codeSystem.setExternalId(term.getOntology_name());
      result.setCodeSystem(codeSystem);

      result.setSynonyms(term.getSynonyms());

      return result;
  }

  public CodePage getCodes(List<String> include, String label, List<String> codeSystemIds, Integer page) {
      return getCodes(include, label, codeSystemIds, page, SEARCH_METHOD.SEARCH);
  }

  public CodePage getCodeSuggestions(List<String> include, String label, List<String> codeSystemIds, Integer page) {
    return getCodes(include, label, codeSystemIds, page, SEARCH_METHOD.SUGGEST);
  }

  private CodePage getCodes(
          List<String> include, String term, List<String> codeSystemIds, Integer page, SEARCH_METHOD searchMethod) {
      OLSSuggestResponseBody response =
              Objects.requireNonNull(
                              terminologyService
                                      .get()
                                      .uri(
                                              uriBuilder ->
                                                      uriBuilder
                                                              .path(searchMethod.getEndpoint())
                                                              .queryParam("q", term)
                                                              .queryParam("start", page == null ? 0 : page - 1) // OLS page count starts from 0, we start from 1
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
                                  Code code = new Code();
                                  code.setCode(responseItem.getLabel());
                                  code.setUri(responseItem.getIri());
                                  code.setCode(responseItem.getLabel());
                                  code.setUri(responseItem.getIri());
                                  var uniqueLabels = new HashSet<String>();
                                  switch (searchMethod) {
                                      case SEARCH:
                                          uniqueLabels.addAll(Optional.ofNullable(responseItem.getAutoSuggestion().getLabel()).orElse(Collections.emptyList()));
                                          uniqueLabels.addAll(Optional.ofNullable(responseItem.getAutoSuggestion().getSynonym()).orElse(Collections.emptyList()));
                                          break;
                                      case SUGGEST:
                                          uniqueLabels.addAll(Optional.ofNullable(responseItem.getAutoSuggestion().getLabel_autosuggest()).orElse(Collections.emptyList()));
                                          uniqueLabels.addAll(Optional.ofNullable(responseItem.getAutoSuggestion().getSynonym_autosuggest()).orElse(Collections.emptyList()));
                                          break;
                                  }
                                  code.setSynonyms(new ArrayList<>(uniqueLabels));
                                  return code;
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
                        .queryParam("page", page == null ? 0 : page - 1)
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
                ontology -> {
                  CodeSystem codeSystem = new CodeSystem();
                  codeSystem.setExternalId(ontology.getOntologyId());
                  codeSystem.setUri(ontology.getConfig().getId());
                  codeSystem.setName(ontology.getConfig().getTitle());
                  return codeSystem;
                })
            .collect(Collectors.toList());

    // TODO: page size, totalElements and page number are wrong because we filtered afterwards. Actually, there is no way of finding out the total number of filtered elements.
    return (CodeSystemPage)
        new CodeSystemPage()
            .content(content)
            .size(response.getPage().getSize())
            .totalElements(response.getPage().getTotalElements())
            .number(response.getPage().getNumber())
            .totalPages(response.getPage().getTotalPages());
  }

}
