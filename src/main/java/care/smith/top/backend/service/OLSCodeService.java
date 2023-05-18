package care.smith.top.backend.service;

import care.smith.top.backend.service.ols.OLSSuggestResponse;
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

    static class OLSOntologiesResponse {
        Embedded _embedded;

    public Embedded get_embedded() {
      return _embedded;
    }

    public void set_embedded(Embedded _embedded) {
      this._embedded = _embedded;
    }

    public Page getPage() {
      return page;
    }

    public void setPage(Page page) {
      this.page = page;
    }
  }

  static class Embedded {
    Ontology[] ontologies;

    public Ontology[] getOntologies() {
      return ontologies;
    }

    public void setOntologies(Ontology[] ontologies) {
      this.ontologies = ontologies;
    }
  }

  static class Ontology {
    String ontologyId;
    OntologyConfig config;

    public String getOntologyId() {
      return ontologyId;
    }

    public void setOntologyId(String ontologyId) {
      this.ontologyId = ontologyId;
    }

    public OntologyConfig getConfig() {
      return config;
    }

    public void setConfig(OntologyConfig config) {
      this.config = config;
    }
  }

  static class OntologyConfig {
    URI id;

    public URI getId() {
      return id;
    }

    public void setId(URI id) {
      this.id = id;
    }
  }

  static class Page {
    int number;
    int size;
    long totalElements;
    int totalPages;

    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }

    public long getTotalElements() {
      return totalElements;
    }

    public void setTotalElements(long totalElements) {
      this.totalElements = totalElements;
    }

    public int getTotalPages() {
      return totalPages;
    }

    public void setTotalPages(int totalPages) {
      this.totalPages = totalPages;
    }
  }

  public CodePage getCode(List<String> include, String term, CodeSystem codeSystems, Integer page) {
    // TODO: do implementation
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
  }

  public CodePage getCodeSuggestions(
      List<String> include, String term, List<String> codeSystems, Integer page) {
    ResponseBody response =
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

    int totalPages = (int) Math.ceil((double) response.numFound / suggestionsPageSize);
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
    // TODO: filter by name and/or URI
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

    List<CodeSystem> content =
        Arrays.stream(Objects.requireNonNull(response).get_embedded().getOntologies())
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
