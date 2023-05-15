package care.smith.top.backend.service;

import care.smith.top.model.Code;
import care.smith.top.model.CodeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
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

  static class OLSSuggestResponse {
    public ResponseBody getResponse() {
      return response;
    }

    public void setResponse(ResponseBody response) {
      this.response = response;
    }

    private ResponseBody response;
  }

  static class ResponseBody {
    int numFound;
    int start;

    public ResponseItem[] getDocs() {
      return docs;
    }

    public void setDocs(ResponseItem[] docs) {
      this.docs = docs;
    }

    ResponseItem[] docs;
  }

  static class ResponseItem {
    String id;
    URI iri;

    public URI getIri() {
      return iri;
    }

    public void setIri(URI iri) {
      this.iri = iri;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    String label;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  static class OLSOntologiesResponse {
    Embedded _embedded;

    public Embedded get_embedded() {
      return _embedded;
    }

    public void set_embedded(Embedded _embedded) {
      this._embedded = _embedded;
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

  public List<Code> getCode(
      List<String> include, String term, CodeSystem codeSystems, Integer page) {
    return null;
  }

  public List<Code> getCodeSuggestions(
      List<String> include, String term, List<String> codeSystems, Integer page) {
    return Arrays.stream(
            terminologyService
                .get()
                .uri(
                    uriBuilder ->
                        uriBuilder
                            .path("/select")
                            .queryParam("q", term)
                            .queryParam("start", page)
                            .queryParam("rows", suggestionsPageSize)
                            .queryParam(
                                "ontology",
                                codeSystems == null ? "" : String.join(",", codeSystems))
                            .build())
                .retrieve()
                .bodyToMono(OLSSuggestResponse.class)
                .block()
                .response
                .docs)
        .map(
            responseItem -> {
              Code code = new Code();
              code.setCode(responseItem.label);
              code.setUri(responseItem.iri);
              return code;
            })
        .collect(Collectors.toList());
  }

  public List<CodeSystem> getCodeSystems(List<String> include, URI uri, String name, Integer page) {
    return Arrays.stream(
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
                .block()
                ._embedded
                .ontologies)
        .map(
            ontology -> {
              CodeSystem codeSystem = new CodeSystem();
              codeSystem.setName(ontology.ontologyId);
              codeSystem.setUri(ontology.config.id);
              return codeSystem;
            })
        .collect(Collectors.toList());
  }
}
