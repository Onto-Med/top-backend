package care.smith.top.backend.api;

import care.smith.top.model.DataSource;
import care.smith.top.backend.service.PhenotypeQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSourceApiDelegateImpl implements DataSourceApiDelegate {
  @Autowired private PhenotypeQueryService queryService;

  @Override
  public ResponseEntity<List<DataSource>> getDataSources() {
    return new ResponseEntity<>(queryService.getDataSources(), HttpStatus.OK);
  }
}
