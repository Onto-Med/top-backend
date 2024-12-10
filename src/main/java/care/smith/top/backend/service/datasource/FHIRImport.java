package care.smith.top.backend.service.datasource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.repository.jpa.datasource.EncounterRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectRepository;
import care.smith.top.backend.repository.jpa.datasource.SubjectResourceRepository;
import care.smith.top.top_phenotypic_query.adapter.fhir.FHIRUtil;
import care.smith.top.top_phenotypic_query.util.DateUtil;
import java.io.Reader;
import java.math.BigDecimal;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Quantity;

public class FHIRImport extends DataImport {

  public FHIRImport(
      String dataSourceId,
      Reader reader,
      SubjectRepository subjectRepository,
      EncounterRepository encounterRepository,
      SubjectResourceRepository subjectResourceRepository) {
    super(dataSourceId, reader, subjectRepository, encounterRepository, subjectResourceRepository);
  }

  @Override
  public void run() {
    IParser parser = FhirContext.forR4().newJsonParser();
    importResource(parser.parseResource(reader));
  }

  private void importResource(IBaseResource r) {
    if (r instanceof Bundle) importBundle((Bundle) r);
    else if (r instanceof Patient) importPatient((Patient) r);
    else if (r instanceof Encounter) importEncounter((Encounter) r);
    else if (r instanceof Observation) importObservation((Observation) r);
    else if (r instanceof Condition) importCondition((Condition) r);
    else if (r instanceof Procedure) importProcedure((Procedure) r);
    else if (r instanceof Medication) importMedication((Medication) r);
    else if (r instanceof MedicationAdministration)
      importMedicationAdministration((MedicationAdministration) r);
    else if (r instanceof MedicationStatement) importMedicationStatement((MedicationStatement) r);
    else if (r instanceof MedicationRequest) importMedicationRequest((MedicationRequest) r);
  }

  private void importBundle(Bundle r) {
    for (BundleEntryComponent comp : r.getEntry()) importResource(comp.getResource());
  }

  private void importPatient(Patient r) {
    SubjectDao dao = new SubjectDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasBirthDate()) dao.birthDate(DateUtil.ofDate(r.getBirthDate()));
    if (r.hasGender()) dao.sex(r.getGender().toCode());
    saveSubject(dao);
  }

  private void importEncounter(Encounter r) {
    EncounterDao dao = new EncounterDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasClass_()) dao.type(r.getClass_().getCode());
    if (r.hasPeriod()) {
      Period p = r.getPeriod();
      if (p.hasStart()) dao.startDateTime(DateUtil.ofDate(p.getStart()));
      if (p.hasEnd()) dao.endDateTime(DateUtil.ofDate(p.getEnd()));
    }
    saveEncounter(dao);
  }

  private void importObservation(Observation r) {
    SubjectResourceDao dao = new SubjectResourceDao(dataSourceId, FHIRUtil.getId(r));
    if (r.hasSubject()) dao.subjectId(FHIRUtil.getId(r.getSubject()));
    if (r.hasEncounter()) dao.encounterId(FHIRUtil.getId(r.getEncounter()));
    if (r.hasCode()) {
      Coding c = r.getCode().getCodingFirstRep();
      dao.codeSystem(c.getSystem());
      dao.code(c.getCode());
    }

    if (r.hasEffectiveDateTimeType())
      dao.dateTime(DateUtil.ofDate(r.getEffectiveDateTimeType().getValue()));
    else if (r.hasEffectiveInstantType())
      dao.dateTime(DateUtil.ofDate(r.getEffectiveInstantType().getValue()));
    else if (r.hasEffectivePeriod()) {
      Period p = r.getEffectivePeriod();
      if (p.hasStart()) dao.startDateTime(DateUtil.ofDate(p.getStart()));
      if (p.hasEnd()) dao.endDateTime(DateUtil.ofDate(p.getEnd()));
    }

    if (r.hasValueBooleanType()) dao.booleanValue(r.getValueBooleanType().getValue());
    else if (r.hasValueCodeableConcept()) {
      Coding c = r.getValueCodeableConcept().getCodingFirstRep();
      dao.textValue(c.getSystem() + "|" + c.getCode());
    } else if (r.hasValueDateTimeType())
      dao.dateTimeValue(DateUtil.ofDate(r.getValueDateTimeType().getValue()));
    else if (r.hasValueIntegerType())
      dao.numberValue(BigDecimal.valueOf(r.getValueIntegerType().getValue()));
    else if (r.hasValueQuantity()) {
      Quantity q = r.getValueQuantity();
      if (q.hasUnit()) dao.unit(q.getUnit());
      if (q.hasValue()) dao.numberValue(q.getValue());
    } else if (r.hasValueStringType()) dao.textValue(r.getValueStringType().asStringValue());

    saveSubjectResource(dao);
  }

  private void importCondition(Condition r) { // TODO Auto-generated method stub
  }

  private void importProcedure(Procedure r) { // TODO Auto-generated method stub
  }

  private void importMedication(Medication r) { // TODO Auto-generated method stub
  }

  private void importMedicationAdministration(
      MedicationAdministration r) { // TODO Auto-generated method stub
  }

  private void importMedicationStatement(MedicationStatement r) { // TODO Auto-generated method stub
  }

  private void importMedicationRequest(MedicationRequest r) { // TODO Auto-generated method stub
  }
}
