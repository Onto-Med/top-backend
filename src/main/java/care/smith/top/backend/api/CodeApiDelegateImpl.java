package care.smith.top.backend.api;

import care.smith.top.backend.service.OLSCodeService;
import care.smith.top.model.Code;
import care.smith.top.model.CodePage;
import care.smith.top.model.CodeScope;
import care.smith.top.model.CodeSystemPage;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author ralph
 */
@Service
public class CodeApiDelegateImpl implements CodeApiDelegate {

  @Autowired private OLSCodeService codeService;

  @Override
  public ResponseEntity<Code> getCode(URI uri, String codeSystemId, CodeScope scope) {
    return ResponseEntity.ok(
        codeService.getCode(
            URI.create(URLDecoder.decode(uri.toString(), Charset.defaultCharset())),
            codeSystemId,
            scope));
  }

  @Override
  public ResponseEntity<CodePage> getCodes(
      List<String> include, String label, List<String> codeSystemIds, Integer page) {
    return ResponseEntity.ok(codeService.getCodes(include, label, codeSystemIds, page));
  }

  @Override
  public ResponseEntity<CodePage> getCodeSuggestions(
      List<String> include, String term, List<String> codeSystemIds, Integer page) {
    return ResponseEntity.ok(codeService.getCodeSuggestions(include, term, codeSystemIds, page));
  }

  @Override
  public ResponseEntity<CodeSystemPage> getCodeSystems(
      List<String> include, URI uri, String name, Integer page) {
    return ResponseEntity.ok(codeService.getCodeSystems(include, uri, name, page));
  }
}
