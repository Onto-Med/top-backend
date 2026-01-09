package care.smith.top.backend.service.datasource;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.ExpectedResultDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.ExpectedResultRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import care.smith.top.model.DataSourceFileType;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;

public abstract class DataImport {

  protected final String dataSourceId;
  protected final Reader reader;

  protected final SubjectRepository subjectRepository;
  protected final EncounterRepository encounterRepository;
  protected final SubjectResourceRepository subjectResourceRepository;
  protected final ExpectedResultRepository expectedResultRepository;

  protected DataImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      ExpectedResultRepository expectedResultRepository) {
    this.dataSourceId = dataSourceId;
    this.reader = reader;
    this.subjectRepository = subjectRepository;
    this.encounterRepository = encounterRepository;
    this.subjectResourceRepository = subjectResourceRepository;
    this.expectedResultRepository = expectedResultRepository;
  }

  public static DataImport getInstance(
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository,
      ExpectedResultRepository expectedResultRepository,
      Reader reader,
      DataSourceFileType fileType,
      String dataSourceId,
      String config)
      throws IOException {
    DataImport importer;
    switch (fileType) {
      case CSV_SUBJECT:
        importer =
            new SubjectCSVImport(
                dataSourceId, reader, subjectRepository, configToCsvFieldMapping(config));
        break;
      case CSV_ENCOUNTER:
        importer =
            new EncounterCSVImport(
                dataSourceId,
                reader,
                subjectRepository,
                encounterRepository,
                configToCsvFieldMapping(config));
        break;
      case CSV_SUBJECT_RESOURCE:
        importer =
            new SubjectResourceCSVImport(
                dataSourceId,
                reader,
                subjectRepository,
                encounterRepository,
                subjectResourceRepository,
                configToCsvFieldMapping(config));
        break;
      case CSV_EXPECTED_RESULT:
        importer =
            new ExpectedResultCSVImport(
                dataSourceId,
                reader,
                subjectRepository,
                encounterRepository,
                subjectResourceRepository,
                expectedResultRepository,
                configToCsvFieldMapping(config));
        break;
      case FHIR:
        importer =
            new FHIRImport(
                dataSourceId,
                reader,
                subjectRepository,
                encounterRepository,
                subjectResourceRepository,
                config != null && config.contains("mergeEncounters=true"));
        break;
      default:
        throw new NotImplementedException(
            "The specified data source file or type are not supported.");
    }
    return importer;
  }

  public static Map<String, String> configToCsvFieldMapping(String config) {
    return configToCsvFieldMapping(config, ";", "=");
  }

  public static Map<String, String> configToCsvFieldMapping(
      String config, @NotNull String listSeparator, @NotNull String keyValueSeparator) {
    if (config == null) return new HashMap<>();
    return Arrays.stream(config.split(listSeparator))
        .map(kv -> kv.split(keyValueSeparator))
        .collect(Collectors.toMap(k -> k[0], v -> v[1]));
  }

  public abstract void run();

  private SubjectDao getSubject(String subjectId) {
    Optional<SubjectDao> subject =
        subjectRepository.findBySubjectKeyDataSourceIdAndSubjectKeySubjectId(
            dataSourceId, subjectId);
    return subject.orElseGet(() -> subjectRepository.save(new SubjectDao(dataSourceId, subjectId)));
  }

  private EncounterDao getEncounter(String encounterId, SubjectDao subject) {
    Optional<EncounterDao> encounter =
        encounterRepository.findByEncounterKeyDataSourceIdAndEncounterKeyEncounterId(
            dataSourceId, encounterId);
    if (encounter.isPresent()) return encounter.get();
    if (subject == null)
      return encounterRepository.save(new EncounterDao(dataSourceId, encounterId));
    return encounterRepository.save(new EncounterDao(dataSourceId, encounterId, subject));
  }

  protected void saveSubject(SubjectDao subject) {
    subjectRepository.save(subject);
  }

  protected void saveEncounter(EncounterDao encounter) {
    if (encounter.getSubjectId() != null) encounter.subject(getSubject(encounter.getSubjectId()));
    encounterRepository.save(encounter);
  }

  protected void saveSubjectResource(SubjectResourceDao subjectResource) {
    SubjectDao subject = null;
    if (subjectResource.getSubjectId() != null) {
      subject = getSubject(subjectResource.getSubjectId());
      subjectResource.subject(subject);
    }

    if (subjectResource.getEncounterId() != null)
      subjectResource.encounter(getEncounter(subjectResource.getEncounterId(), subject));

    subjectResourceRepository.save(subjectResource);
  }

  protected void saveExpectedResult(ExpectedResultDao expectedResult) {
    SubjectDao subject = null;
    if (expectedResult.getSubjectId() != null) {
      subject = getSubject(expectedResult.getSubjectId());
      expectedResult.subject(subject);
    }

    if (expectedResult.getEncounterId() != null)
      expectedResult.encounter(getEncounter(expectedResult.getEncounterId(), subject));

    expectedResultRepository.save(expectedResult);
  }
}
