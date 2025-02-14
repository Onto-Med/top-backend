package care.smith.top.backend.service;

import care.smith.top.backend.repository.ols.CodeRepository;
import care.smith.top.backend.repository.ols.CodeSystemRepository;
import care.smith.top.backend.repository.ols.OlsRepository;
import care.smith.top.model.*;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @author ralph
 */
@Service
@Primary
public class OLSCodeService {
  @Autowired protected CodeSystemRepository codeSystemRepository;
  @Autowired protected CodeRepository codeRepository;

  @Value("${spring.paging.page-size:10}")
  private int ontologyPageSize;

  @NotNull
  private static Predicate<CodeSystem> filterByName(String name) {
    return cs ->
        name == null
            || cs.getName() != null && StringUtils.containsIgnoreCase(cs.getName(), name)
            || cs.getShortName() != null && StringUtils.containsIgnoreCase(cs.getShortName(), name);
  }

  public Code getCode(URI uri, String codeSystemId, CodeScope scope) {
    return codeRepository.getCode(uri, codeSystemId, scope);
  }

  public CodePage getCodes(
      List<String> include, String label, List<String> codeSystemIds, Integer page) {
    return codeRepository.getCodes(label, codeSystemIds, page, OlsRepository.SEARCH_METHOD.SEARCH);
  }

  public CodePage getCodeSuggestions(
      List<String> include, String label, List<String> codeSystemIds, Integer page) {
    return codeRepository.getCodes(label, codeSystemIds, page, OlsRepository.SEARCH_METHOD.SUGGEST);
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

    List<CodeSystem> filteredCodeSystems =
        codeSystemRepository.getAllCodeSystems().values().stream()
            .sorted((a, b) -> a.getExternalId().compareToIgnoreCase(b.getExternalId()))
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
}
