package care.smith.top.backend.api;

import care.smith.top.backend.service.OLSCodeService;
import care.smith.top.model.Code;
import care.smith.top.model.CodeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

/**
 * @author ralph
 */
@Service
public class CodeApiDelegateImpl implements CodeApiDelegate {

    @Autowired private OLSCodeService codeService;

    @Override
    public ResponseEntity<List<Code>> getCode(List<String> include, String term, CodeSystem codeSystems, Integer page) {
        return new ResponseEntity<>(codeService.getCode(include, term, codeSystems, page), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Code>> getCodeSuggestions(List<String> include, String term, List<String> codeSystems, Integer page) {
        return new ResponseEntity<>(codeService.getCodeSuggestions(include, term, codeSystems, page), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<CodeSystem>> getCodeSystems(List<String> include, URI uri, String name, Integer page) {
        return new ResponseEntity<>(codeService.getCodeSystems(include, uri, name, page), HttpStatus.OK);
    }
}
