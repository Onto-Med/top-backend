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
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author ralph
 */
@Service
public class OLSCodeService {

  private WebClient terminologyService;

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

  public CodePage getCodes(List<String> include, String label, String codeSystemId, Integer page) {
    // TODO: do implementation
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
  }

  public CodePage getCodeSuggestions(
      List<String> include, String term, List<String> codeSystems, Integer page) {
    OLSSuggestResponseBody response =
        Objects.requireNonNull(
                terminologyService
                    .get()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path("/select")
                                .queryParam("q", term)
                                .queryParam("start", page == null ? 0 : page - 1)
                                .queryParam("rows", suggestionsPageSize)
                                .queryParam(
                                    "ontology",
                                    codeSystems == null ? "" : String.join(",", codeSystems))
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
            .filter(ontology -> uri != null && ontology.getConfig().getId().equals(uri))
            .filter(ontology -> name != null && ontology.getConfig().getTitle().equals(name))
            .map(
                ontology -> {
                  CodeSystem codeSystem = new CodeSystem();
                  codeSystem.setName(ontology.getOntologyId());
                  codeSystem.setUri(ontology.getConfig().getId());
                  return codeSystem;
                })
            .collect(Collectors.toList());

    return (CodeSystemPage)
        new CodeSystemPage()
            .content(content)
            .size(response.getPage().getSize())
            .totalElements(response.getPage().getTotalElements())
            .number(response.getPage().getNumber())
            .totalPages(response.getPage().getTotalPages());
  }

}
