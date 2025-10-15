package care.smith.top.backend.repository.ols;

import care.smith.top.backend.service.ols.*;
import care.smith.top.model.Code;
import care.smith.top.model.CodePage;
import care.smith.top.model.CodeScope;
import care.smith.top.model.CodeSystem;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Repository
public class OlsCodeRepository extends OlsRepository {
  private static final java.util.logging.Logger LOGGER =
      Logger.getLogger(OlsCodeRepository.class.getName());

  @Value("${coding.suggestions-page-size}")
  private int suggestionsPageSize;

  @Value("${coding.code-children-page-size}")
  private int codeChildrenPageSize;

  @Autowired private OlsCodeSystemRepository olsCodeSystemRepository;

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

  public Code getCode(URI uri, String codeSystemId, CodeScope scope) {
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
                        .pathSegment(
                            "ontologies",
                            codeSystemId,
                            "terms",
                            URLEncoder.encode(uri.toString(), Charset.defaultCharset()))
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

    String primaryLabel =
        term.getSynonyms() != null && term.getSynonyms().size() != 0
            ? term.getSynonyms().get(0)
            : term.getLabel();

    Code result =
        new Code()
            .code(term.getLabel())
            .name(primaryLabel)
            .uri(URI.create(term.getIri()))
            .codeSystem(codeSystem)
            .synonyms(term.getSynonyms());

    var childrenLink = term.get_links().getHierarchicalChildren();
    if (childrenLink != null) {
      fillInSubtree(result, scope);
    }

    if (result.getChildren() == null) {
      result.setChildren(Collections.emptyList());
    }
    return result;
  }

  private void fillInSubtree(Code code, CodeScope scope) {
    switch (scope) {
      case LEAVES:
        code.setChildren(collectLeaves(code, new HashSet<>()));
        return;
      case SUBTREE:
        fillInSubtree(code, new HashSet<>());
        return;
      case SELF:
      default:
    }
  }

  private void fillInSubtree(Code code, Set<URI> tracker) {
    // We need to keep track of which codes we have already encountered as ontologies might
    // contain cycles, and we might end up in an infinite loop.
    // This is not supposed to happen with TOP terminologies created by the
    // TOP import pipeline. However, we have no control over what kind of ontologies are
    // loaded into the OLS instance, so better safe than sorry.
    if (tracker.contains(code.getUri())) return;
    tracker.add(code.getUri());

    var children = retrieveChildren(code);

    code.setChildren(children);
    children.forEach(
        childCode -> {
          fillInSubtree(childCode, tracker);
        });
  }

  private List<Code> collectLeaves(Code code, Set<URI> tracker) {
    if (tracker.contains(code.getUri())) return Collections.emptyList();
    tracker.add(code.getUri());

    var children = retrieveChildren(code);

    if (children.isEmpty()) {
      // code is a leaf.
      return List.of(code);
    }

    return children.stream()
        .flatMap(childCode -> collectLeaves(childCode, tracker).stream().distinct())
        .collect(Collectors.toList());
  }

  class PageCounter {
    private Integer page = 0;

    Integer getPage() {
      return page;
    }

    void increase() {
      page++;
    }
  }

  private List<Code> retrieveChildren(Code code) {
    final PageCounter counter = new PageCounter();

    final List<Code> result = new ArrayList<>();

    while (true) {
      OLSHierarchicalChildrenResponse response =
          Objects.requireNonNull(
              terminologyService
                  .get()
                  .uri(
                      uriBuilder ->
                          uriBuilder
                              .pathSegment(
                                  "ontologies",
                                  code.getCodeSystem().getExternalId(),
                                  "terms",
                                  URLEncoder.encode(
                                      code.getUri().toString(), Charset.defaultCharset()),
                                  "hierarchicalChildren")
                              .queryParam("page", counter.getPage())
                              .queryParam("size", codeChildrenPageSize)
                              .build())
                  .retrieve()
                  .bodyToMono(OLSHierarchicalChildrenResponse.class)
                  .onErrorResume(
                      WebClientResponseException.class,
                      e ->
                          e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()
                              ? Mono.empty()
                              : Mono.error(e))
                  .block());

      if (response.get_embedded() == null) {
        return Collections.emptyList();
      }
      response
          .get_embedded()
          .getTerms()
          .forEach(
              term -> {
                String primaryLabel =
                    term.getSynonyms() != null && term.getSynonyms().size() != 0
                        ? term.getSynonyms().get(0)
                        : term.getLabel();

                result.add(
                    new Code()
                        .code(term.getLabel())
                        .name(primaryLabel)
                        .uri(URI.create(term.getIri()))
                        .codeSystem(code.getCodeSystem())
                        .synonyms(term.getSynonyms())
                        .children(Collections.emptyList()));
              });

      var paginationInfo = response.getPage();
      if (paginationInfo.getNumber() + 1 == paginationInfo.getTotalPages()) {
        break;
      }
      counter.increase();
    }
    return result;
  }

  public CodePage getCodes(
      String term, List<String> codeSystemIds, Integer page, SEARCH_METHOD searchMethod)
      throws OlsConnectionException {
    OLSSuggestResponseBody response = null;
    try {
      response =
          Objects.requireNonNull(
                  terminologyService
                      .get()
                      .uri(
                          uriBuilder ->
                              uriBuilder
                                  .path(searchMethod.getEndpoint())
                                  .queryParam("q", term)
                                  .queryParam(
                                      "fieldList", "id,iri,short_form,ontology_name,label,synonym")
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
    } catch (Exception e) {
      LOGGER.severe("Could not retrieve codes from terminology server: " + e.getMessage());
      throw new OlsConnectionException(e);
    }

    int totalPages = (int) Math.ceil((double) response.getNumFound() / suggestionsPageSize);
    List<Code> content =
        Arrays.stream(response.getDocs())
            .map(
                responseItem -> {
                  String primaryLabel =
                      responseItem.getSynonym() != null && responseItem.getSynonym().size() != 0
                          ? responseItem.getSynonym().get(0)
                          : responseItem.getLabel();

                  return new Code()
                      .name(primaryLabel)
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

  public Optional<CodeSystem> getCodeSystem(String externalId) {
    if (externalId == null) return Optional.empty();
    try {
      return olsCodeSystemRepository.getAllCodeSystems().values().stream()
          .filter(cs -> externalId.equals(cs.getExternalId()))
          .findFirst();
    } catch (OlsConnectionException e) {
      return Optional.empty();
    }
  }

  public Optional<CodeSystem> getCodeSystem(URI codeSystemUri) {
    try {
      if (olsCodeSystemRepository.getAllCodeSystems().containsKey(codeSystemUri))
        return Optional.of(olsCodeSystemRepository.getAllCodeSystems().get(codeSystemUri));
    } catch (OlsConnectionException ignored) {
    }
    return Optional.empty();
  }
}
