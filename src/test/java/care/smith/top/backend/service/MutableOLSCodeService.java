package care.smith.top.backend.service;

import org.springframework.stereotype.Service;

/**
 * @author Ralph Schäfermeier
 */
@Service
public class MutableOLSCodeService extends OLSCodeService {
    public void setEndpoint(String endpoint) {
        codeSystemRepository.setTerminologyServiceEndpoint(endpoint);
        codeRepository.setTerminologyServiceEndpoint(endpoint);
    }
}
